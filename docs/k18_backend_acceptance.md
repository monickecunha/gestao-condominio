# K18 — Homologação do backend

## Escopo e base

[Card K18](https://github.com/monickecunha/gestao-condominio/issues/18). Homologação dos 13 critérios de [planning.md §7](planning.md#7-critérios-de-sucesso-do-mvp), com backend/API e H2. Frontend e PostgreSQL não são requisitos desta entrega.

Base de integração: `dev` em `89ae045`, obtida em 15/11/2026, incluindo K13, K14, K15, K16 e K17. Branch de trabalho: `feature/k18-homologacao-backend`. Dados usados são fictícios. A matriz abaixo identifica os testes executáveis de cada requisito; os resultados da execução estão registrados adiante.

## Matriz dos 13 critérios

| Nº | Critério | Evidência HTTP / teste com H2 | Resultado esperado |
|---|---|---|---|
| 1 | Cadastro e autenticação de morador | `BindingRegistrationHttpTests.createsPendingBindingAndAllowsLoginButDeniesComplaint` | Cadastro 201, vínculo pendente, login 200; denúncia negada até aprovação. |
| 2 | Denúncia autorizada sem associação ao autor | `SecurityHttpTests.registersAnonymousComplaintsWithDistinctProtocols`; `PrivacyAbuseHttpTests.preservesPrivacyInDatabaseResponsesAndLogs` | 201; vínculo aprovado no momento do envio; tabela sem usuário, unidade ou origem. |
| 3 | Validação dos dados obrigatórios | `SecurityHttpTests.invalidOrUnauthorizedComplaintsDoNotWriteRows` | 400 para entradas inválidas e nenhuma gravação parcial. |
| 4 | Categoria e imagens opcionais | `ComplaintImagesHttpTests.acceptsMultipartWithoutImagesAndKeepsJsonSubmission`, `persistsAndReturnsSanitizedJpegPngAndWebp` e negativas da mesma classe | JSON ou multipart válido com/sem imagens; rejeição de arquivos inválidos e limites excedidos. |
| 5 | Protocolo após registro | `SecurityHttpTests.registersAnonymousComplaintsWithDistinctProtocols`; coleção geral, passos 32–33 | Protocolos UUID v4 distintos; recibo sem identidade. |
| 6 | Consulta pelo protocolo | `ComplaintTrackingHttpTests.tracksComplaintWithoutLoginAndShowsOnlyPublicFields` | 200 público com protocolo conhecido; não exige token de login. |
| 7 | Login e acesso administrativo | `SecurityHttpTests.loginAndInvalidCredentials`, `rolesAndCurrentBinding` | Credenciais válidas emitem token; perfil e vínculo atual controlam acesso. |
| 8 | Lista do condomínio autenticado | `ComplaintManagementHttpTests.managerListsOwnComplaintsWithPagination`, `otherCondominiumManagerSeesOnlyOwnDataEvenByDirectId` | Paginação e isolamento por condomínio. |
| 9 | Detalhe e imagens administrativas | `ComplaintManagementHttpTests.managerSeesFullDetailsOfOwnComplaint`; `ComplaintImagesHttpTests.requiresAnApprovedResidentForMultipartAndTheOwningManagerForRetrieval` | Detalhe/bytes acessíveis somente ao síndico autorizado. |
| 10 | Atualizar status e registrar providências | `ComplaintLifecycleHttpTests.residentReportsManagerHandlesAndResidentFollowsByProtocol`; `SecurityHttpTests.resolvesComplaintWithPrivateActionAndPublicHistory` | Fluxo RECEIVED → UNDER_REVIEW → IN_PROGRESS → RESOLVED; providência interna registrada. |
| 11 | Histórico das alterações | `ComplaintTrackingHttpTests.returnsStatusHistoryAndUsesLastChangeAsUpdatedAt`; `SecurityHttpTests.concurrentReviewAndJustifiedRejectionProduceExactlyTwoEntries` | Histórico ordenado, atualização coerente e uma entrada por transição efetiva. |
| 12 | Acompanhamento atualizado | `ComplaintLifecycleHttpTests`; coleção geral, passos 63 e 67 | Consulta pública reflete resolução/rejeição e justificativa pública, sem providências internas. |
| 13 | Registro e consulta sem exposição direta do autor | `PrivacyAbuseHttpTests`; `ComplaintTrackingHttpTests.complaintTableHasNoLinkToTheAuthor`; `ComplaintImagesHttpTests` | Sem vínculo persistido autor/denúncia, logs sensíveis ou metadados originais nas imagens sanitizadas. |

## Cobertura complementar exigida pela K18

- **Pendentes e rejeitados:** `BindingRegistrationHttpTests`, `SecurityHttpTests` e coleção geral 7–31. Pendente autentica, mas não denuncia; vínculo rejeitado não autoriza envio e todos os vínculos rejeitados impedem login naquele condomínio.
- **Convites/cadastro adicional:** `BindingRegistrationHttpTests.primaryRegistersAdditionalResidentWhoRequiresManagerApproval`, `deniesOtherUnitAndCallersWhoAreNotItsApprovedPrimaryResident` e `rejectsDuplicateAdditionalRegistrationWithoutChangingAccountOrBinding`. No MVP, convite significa cadastro por `POST /bindings/additional` pelo responsável aprovado; não há envio de e-mail ou link de convite. O novo vínculo depende da aprovação do síndico.
- **Imagens:** seis testes de `ComplaintImagesHttpTests` e [coleção K14](card-collections/k14/README.md), incluindo conteúdo, metadados, tamanho, quantidade e acesso ao download.
- **Filtros:** sete testes de `ComplaintFilterHttpTests` e [coleção K15](card-collections/k15/README.md), com datas inclusivas de Brasília, paginação e isolamento.
- **Transições e acessos proibidos:** `ComplaintLifecycleHttpTests`, `SecurityHttpTests`, `ComplaintManagementHttpTests` e coleções. Inclui rejeição justificada, pulo de etapa, tentativa de reabrir resolução, token ausente e usuário de outro condomínio.
- **Abuso:** `RequestWindowTests`, `PrivacyAbuseHttpTests` e [coleção K17](card-collections/k17/README.md). Preservar cotas padrão na validação das coleções.

## Coleção independente da K18

Para executar um único roteiro de homologação com preparação própria, use a [coleção K18](card-collections/k18/README.md). O README lista os nomes das requisições, resultados esperados e relação com os 13 critérios, incluindo cadastro adicional e imagens.

## Reprodução

Na raiz, configure `JAVA_HOME` para JDK 25:

```bash
bash mvnw --batch-mode --no-transfer-progress clean verify
java -jar target/gestao-condominio-0.0.1-SNAPSHOT.jar --spring.profiles.active=setup
```

No PowerShell, use `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify` e o mesmo comando `java -jar`.

O primeiro comando executa os testes em H2 isolado e gera o JAR. O segundo inicia a API na porta 8080 com H2 vazio. Não requer frontend, PostgreSQL ou serviço externo; o primeiro build precisa baixar dependências Maven. Para usar outra porta, acrescente `--server.port=8081` e ajuste `baseUrl` no Postman.

Execute a [coleção geral](condominium-management.postman_collection.json) e as coleções K14, K15 e K17 conforme seus READMEs. **Encerre e reinicie a API antes de cada coleção**, pois cada roteiro prepara seus próprios cadastros e consome cotas. Na K14, configure `docs/card-collections/k14` como diretório de trabalho para localizar `sample-image.png`. Na K17, as esperas de 61 segundos são automáticas; execute uma iteração e permita scripts de mais de 61 segundos.

As coleções pulam cenários opcionais que precisam de fixtures de outro condomínio. A evidência obrigatória desse isolamento vem dos testes Java com dois condomínios no mesmo H2 de teste; a restrição de um condomínio por instalação permanece na aplicação.

## Resultado da suíte integrada — 15/11/2026

`JAVA_HOME` configurado para Temurin JDK 25; comando `bash mvnw --batch-mode --no-transfer-progress clean verify`: **81 testes, zero falhas, zero erros e zero testes ignorados; BUILD SUCCESS**. Os testes das 12 classes executadas cobrem a matriz acima e as negativas complementares. O JAR foi empacotado a partir dessa mesma base.

## Validação do JAR e coleções

O JAR empacotado foi iniciado com perfil `setup`, sem configurar banco externo ou frontend, em instâncias independentes com H2 vazio e cotas padrão. As coleções foram executadas com Newman; a K14 usou seu diretório de trabalho para carregar a imagem fictícia. As instâncias temporárias foram encerradas após cada execução.

| Roteiro | Requisições | Asserções | Resultado |
|---|---:|---:|---|
| Geral | 94 | 138 | Aprovado |
| K14 — imagens | 15 | 20 | Aprovado |
| K15 — filtros | 34 | 59 | Aprovado |
| K17 — privacidade e abuso | 88 | 94 | Aprovado |

Total das quatro coleções: **231 requisições e 311 asserções, sem falhas**. Não foi identificada falha impeditiva nos cenários exercitados; não houve alteração no código funcional. Os resultados não constituem teste de carga ou certificação de segurança fora do escopo local do MVP.

Na coleção geral, os passos opcionais 23, 51, 52 e 87 não executam sem suas fixtures; na K15, o passo 35 de outro condomínio é opcional. Não contam como evidência de isolamento; essa validação foi executada na suíte Java. As quantidades acima incluem chamadas internas dos scripts.

A coleção específica K18 também foi validada separadamente em H2 vazio: **75 requisições e 107 asserções, sem falhas**, incluindo cadastro adicional e anexos no mesmo roteiro.

## Correções de documentação na integração

O guia K13 recém-integrado ainda apontava para o nome antigo da coleção geral e tratava K14 como futura. Corrigidos o link e a indicação da coleção de imagens disponível; arquivo renomeado para `k13_administrative_workflow.md`, seguindo os nomes em inglês adotados no projeto. Não há mudança de contrato HTTP por essa correção.

## Pendências de conclusão humana

- Registrar a revisão cruzada por outro integrante e o PR desta homologação. Testes automáticos e revisão do próprio autor não comprovam esse item.
- Registrar a decisão de congelamento de funcionalidades com a data real. A data planejada de 12/11 já passou; não declarar congelamento retroativo sem evidência. A partir do congelamento, alterações devem se limitar às correções necessárias para entrega.
- Integrar o PR e registrar eventuais correções posteriores. Se código funcional mudar, repetir as verificações afetadas antes de concluir o card.

A execução técnica não marca automaticamente a K18 como Done nem substitui essas confirmações da equipe.
