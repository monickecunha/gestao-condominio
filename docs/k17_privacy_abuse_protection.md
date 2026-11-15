# K17 — Privacidade e proteção contra abuso

## Escopo e revisão

Cartão: [K17 — Validar privacidade e proteção contra abuso](https://github.com/monickecunha/gestao-condominio/issues/17).

Implementação sobre `dev` no commit `bc279c2`, incluindo K14 e K16. A revisão cobre o backend/API com H2, sem exigir frontend ou banco externo. Evidências usam dados fictícios; não publique tokens, senhas, CPFs, protocolos reais ou logs de requisições em PRs.

Foram conferidos os fluxos de autenticação, cadastros, denúncia JSON/multipart, listagem/detalhe, histórico público, providências e download de imagens. A autorização continua derivando usuário, perfil e condomínio do JWT e verificando permissões atuais no banco. K16 também exige responsável aprovado da unidade no serviço, antes de criar o vínculo adicional.

## Alterações

- Cotas temporárias por origem para login (30/minuto), denúncias JSON/multipart (10/minuto) e cadastros (20/minuto compartilhados entre condomínio, vínculo público e morador adicional).
- Preservado o limite de acompanhamento público de 10/minuto. O contador anterior foi substituído por uma implementação com capacidade limitada, expiração e operação atômica para chamadas concorrentes.
- Cada grupo mantém até 10.000 origens em memória. Ao atingir a capacidade, novas origens são recusadas enquanto não houver espaço; contadores ativos não são descartados para liberar novas tentativas.
- 429 usa o envelope padrão e inclui `Retry-After: 60` e `Cache-Control: no-store`. Tentativas inválidas também contam; excesso é recusado antes de interpretar o corpo e autenticar nas rotas POST.
- JSON inválido no login recebe mensagem genérica, evitando que mensagens internas de conversão reproduzam valores enviados pelo cliente em respostas/logs.
- Logs SQL, parâmetros JDBC, mensagens de erro JDBC que podem conter valores de constraints e access log desabilitados por padrão. Não foi acrescentado logging de CPF, token, senha, IP ou conteúdo de denúncias.
- Corrigida a explicação da K01: nomes UUID substituem nomes originais; a remoção de metadados depende do tratamento dos bytes, já implementado na K14.

Os limites e configurações estão no [contrato HTTP](api_contract.md#proteção-contra-abuso-k17). Cadastros adicionais compartilham a cota de cadastros; não compartilham a cota de denúncias.

## Matriz de evidências

| Critério do card | Evidência automatizada / revisão |
|---|---|
| Dados no H2 sem associação direta ao autor | `PrivacyAbuseHttpTests.preservesPrivacyInDatabaseResponsesAndLogs` confere todas as colunas de `tb_complaint`, associação ao condomínio e ausência de campos de usuário/unidade/origem. K08 também verifica o esquema. Não foi criada tabela de auditoria de autores nem tabela de limitação. |
| Respostas públicas e administrativas | O mesmo teste verifica recibo, campos públicos, ausência de conteúdo privado e de identificação do morador. `SecurityHttpTests.resolvesComplaintWithPrivateActionAndPublicHistory` cobre a separação entre histórico público e providência interna. |
| Logs | `preservesPrivacyInDatabaseResponsesAndLogs`, `malformedInputDoesNotEchoOrLogSensitiveValues` e `persistenceFailuresDoNotLogIdentifyingValues` capturam a saída da aplicação em operações HTTP, conversões inválidas e violação de unicidade. Não devem aparecer credenciais, protocolo, conteúdo privado ou identificadores enviados nos casos exercitados. |
| Anexos | `ComplaintImagesHttpTests` verifica remoção de metadados de JPEG, PNG e WebP, nomes UUID, conteúdo recuperado, limites, MIME incompatível, truncamento, rollback e ausência de anexos na consulta pública. |
| Dois condomínios e acessos proibidos | `PrivacyAbuseHttpTests`, `SecurityHttpTests`, `ComplaintManagementHttpTests` e `ComplaintImagesHttpTests` verificam isolamento de listas, detalhe, alterações e download. As fixtures relaxam a constraint de condomínio único apenas nos bancos de teste isolados. |
| Limites de autenticação, registro e consulta | `PrivacyAbuseHttpTests` verifica 429, cabeçalhos, tentativas inválidas, compartilhamento JSON/multipart, cadastros, protocolos inexistentes e ausência de novas denúncias após o bloqueio. |
| Concorrência, expiração e memória limitada | `RequestWindowTests` verifica cota sob 50 chamadas concorrentes, recuperação na fronteira exata de um minuto e recusa de novas origens quando a capacidade está ocupada. Usa relógio controlado, sem aguardar um minuto real. |
| Entradas maliciosas | Texto com marcação HTML e fragmento SQL é tratado como dado, armazenado sem execução e restrito ao detalhe administrativo. Enum/tipo JSON inválido retorna 400 sem eco do valor. Alterar `Forwarded`/`X-Forwarded-For` não renova a cota. Arquivos falsos e inválidos são cobertos pela K14. |

Os testes HTTP existentes isolam seus contadores no início de cada cenário. Os testes específicos K17 mantêm a cota durante cada cenário para comprovar o bloqueio. Isso evita que a ordem da suíte determine o resultado de um teste de outro card.

## Executar

Para validação HTTP independente, use a [coleção Postman específica da K17](card-collections/k17/README.md), com preparação e resultados esperados por requisição.

Use JDK 25 com `JAVA_HOME` configurado, na raiz do projeto:

```bash
./mvnw -Dtest=RequestWindowTests,PrivacyAbuseHttpTests,ComplaintImagesHttpTests,SecurityHttpTests test
./mvnw clean verify
```

No PowerShell, coloque `"-Dtest=RequestWindowTests,PrivacyAbuseHttpTests,ComplaintImagesHttpTests,SecurityHttpTests"` entre aspas e use `.\mvnw.cmd`.

## Resultado da validação — 15/11/2026

- `bash mvnw --batch-mode --no-transfer-progress clean verify`, com Temurin JDK 25: **77 testes, nenhuma falha, erro ou teste ignorado; BUILD SUCCESS**. Inclui os dez testes novos da K17 e as regressões existentes.
- Coleção geral `condominium-management.postman_collection.json`, executada com Newman contra o JAR gerado, perfil `setup`, H2 vazio e cotas padrão: **94 requisições e 138 asserções, nenhuma falha**. Os passos opcionais sem fixture (23, 51, 52 e 87) foram pulados pelos scripts da coleção. O isolamento de dois condomínios foi exercitado pela suíte Java.
- A primeira tentativa da coleção começou antes de a API concluir a inicialização e falhou por conexão recusada. A execução válida acima aguardou a disponibilidade do servidor e usou uma instância/banco novos; não foi necessário alterar o código ou a coleção.
- `git diff --check`: sem erros de whitespace.

## Limites e operação

- Não há armazenamento persistente de origem nem associação do contador com usuário ou denúncia. Em memória, cada entrada guarda apenas origem, contagem e expiração. O acesso após expiração inicia nova janela; uma limpeza periódica remove entradas ociosas expiradas em até aproximadamente mais um minuto. Reiniciar limpa todos os contadores.
- As cotas são por instância e IP direto. Usuários em uma mesma rede/NAT compartilham cotas. Cabeçalhos de encaminhamento do cliente são ignorados (`server.forward-headers-strategy=none`). Uma implantação com proxy ou múltiplas instâncias exige configuração própria de confiança e limitação compartilhada.
- A proteção reduz abuso no escopo local do MVP; não equivale a proteção contra ataque distribuído. Os limites podem ser ajustados às condições da demonstração pelas propriedades documentadas.
- Não habilite logs de corpo, tokens, parâmetros SQL ou access logs contendo protocolo/origem na demonstração. A garantia testada se refere à configuração fornecida; proxies, ferramentas de observabilidade e configurações externas precisam de revisão própria.
- Anonimato significa ausência de associação direta persistida pelo sistema. Texto, pixels de imagens e justificativas públicas podem identificar pessoas pelo conteúdo fornecido; remover EXIF não remove informações visíveis na imagem. Um eventual frontend deve renderizar texto como texto, sem executar HTML armazenado.

