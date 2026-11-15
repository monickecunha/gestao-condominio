# AGENTS.md — Gestão de Condomínio

## Escopo e fluxo de trabalho

Use estas instruções na raiz e consulte também as instruções aplicáveis aos subdiretórios afetados.
Pelo objetivo do pedido, aplique o fluxo correspondente:

| Pedido | Fluxo |
| --- | --- |
| Implementar issue, história ou tarefa técnica | Implementação e validação dos critérios de aceite |
| Corrigir bug, manter ou refatorar | Implementação com diagnóstico e verificação de regressão |
| Revisar diff local ou PR | Code Review consultivo e, para PR, leitura do contexto no GitHub |
| Ensaiar a integração de um PR | PR Trial com a combinação proposta de base e origem |
| Decidir se a alteração pode ser integrada | Pre-Merge Review com evidências da revisão, do trial e dos checks aplicáveis |

## Projeto e fontes

O MVP entrega uma API REST de denúncias anônimas em condomínio, do cadastro e da aprovação
do morador ao acompanhamento por protocolo e à gestão pelo síndico. O backend usa Java 25,
Spring Boot, Spring MVC, Spring Security com JWT e Spring Data JPA/Hibernate. O build usa
Maven Wrapper e a execução local e os testes usam H2. Frontend e banco externo são opcionais
no escopo do MVP. Confirme versões no [pom.xml](pom.xml) e o ambiente no [README](README.md).

Consulte apenas as fontes pertinentes à tarefa:

- **Escopo e aceite:** issue/cartão, [planejamento](docs/planning.md), [histórias](docs/userStories.md),
  [regras RN01–RN13](docs/businessRules.md) e [Kanban](docs/kanban.md).
- **API e decisões:** [contrato HTTP](docs/api_contract.md) e [decisões K01](docs/k01_technical_decisions_summary.md).
- **Fluxos sensíveis:** [K04 — segurança](docs/k04_authentication_authorization.md),
  [K05 — cadastro e unidades](docs/k05_initial_registration.md) e [K06 — vínculos](docs/k06_unit_bindings.md).
- **Persistência:** [arquitetura K03](docs/architecture_and_entities.md) e [modelo relacional](docs/condominium_er_diagram.md).
- **Validação:** [guia de testes](docs/testing.md), [CI](.github/workflows/build.yml)
  e [coleção Postman](docs/condominium-management.postman_collection.json).

Priorize instruções explícitas e requisitos da tarefa, respeitando instruções superiores.
Para entender a intenção do projeto, siga a documentação pertinente, as decisões aprovadas,
as configurações executáveis e testes, os padrões consistentes no código e, por fim, boas práticas compatíveis.
Confronte contrato, código e testes para identificar o comportamento observado. Trate planos e rotas
mencionadas em decisões como propostas até confirmar a implementação na revisão analisada.
Registre divergências relevantes com suas fontes. Se a decisão mudar materialmente uma regra de negócio,
a segurança ou um contrato e a intenção estiver indefinida, esclareça esse ponto antes da mudança.

## Arquitetura e desenvolvimento

O código fica em [src/main/java/residencia/gestao_condominio](src/main/java/residencia/gestao_condominio/).
Siga o fluxo HTTP → controller/DTO → serviço → repositório/entidade:

- Use `controllers` para mapeamento HTTP e validação dos DTOs. Preserve records para o tráfego
  da API quando compatíveis e devolva DTOs próprios para cada escopo de acesso.
- Use `services` para regras, coordenação de gravações e limites transacionais, e `repositories`
  para consultas Spring Data JPA. Filtre e pagine no banco quando a operação for uma listagem paginada.
- Em `entities`, preserve tabelas `tb_` em snake_case, IDs `Long`/`IDENTITY`, enums como texto,
  herança `JOINED` de `User` e as restrições do [modelo relacional](docs/condominium_er_diagram.md).
  Compare mapeamento e constraints com os dados já gravados antes de alterar a modelagem.
- Use `security` para a política HTTP. Mantenha exceções de negócio na camada de serviço e
  traduza erros no tratador HTTP ou de segurança pertinente. Preserve o envelope, os códigos
  HTTP e os cabeçalhos aplicáveis de `/api/v1`. Confira o tratamento real do caminho alterado.

Siga a nomenclatura e o estilo do pacote. Reutilize abstrações e validadores existentes quando
couberem. Prefira a menor alteração correta e sustentável, preservando consumidores e fluxos
adjacentes. Introduza dependências, camadas e alterações de configuração só com necessidade demonstrada.

## Implementação, correções e manutenção

1. Inspecione o estado do Git e as alterações existentes. Identifique critérios de aceite,
   pontos de entrada, consumidores e dependências. Use um plano proporcional à mudança.
2. Em bugs, reproduza a falha quando viável e identifique a causa. Em refatorações, delimite
   o comportamento que precisa ser preservado. Evite misturar correções com melhorias opcionais.
3. Implemente o escopo solicitado, incluindo falhas, limites e efeitos de nova tentativa ou
   concorrência quando afetarem a consistência. Atualize testes para cobrir mudanças observáveis
   ou regressões relevantes que a suíte atual deixaria passar.
4. Confira cada critério com testes ou uma verificação observável, revise o diff e atualize
   a documentação afetada. No relato, indique arquivos, critérios atendidos, comandos e resultados
   reais, verificações pendentes e limitações. Critério obrigatório pendente impede concluir a tarefa.

## Testes e execução local

Execute na raiz com JDK 25 e `JAVA_HOME` configurado. Veja o [guia de testes](docs/testing.md)
para a suíte e o [README](README.md) para iniciar a aplicação.

| Verificação | Linux/macOS | PowerShell |
| --- | --- | --- |
| Ambiente | `./mvnw -version` | `.\mvnw.cmd -version` |
| Persistência | `./mvnw -Dtest=DomainPersistenceTests test` | `.\mvnw.cmd "-Dtest=DomainPersistenceTests" test` |
| Segurança HTTP | `./mvnw -Dtest=SecurityHttpTests test` | `.\mvnw.cmd "-Dtest=SecurityHttpTests" test` |
| Suíte e empacotamento | `./mvnw clean verify` | `.\mvnw.cmd clean verify` |

- Comece pela classe pertinente em [src/test](src/test/). Use o padrão JUnit/Spring Boot:
  testes de persistência para constraints e testes HTTP com servidor em porta aleatória e H2 isolado
  para contrato, permissões e efeitos no banco. Preserve negativas e casos de erro relevantes.
- Amplie para `clean verify` quando a mudança cruzar camadas, afetar segurança ou for candidata
  à integração. A CI declarada roda em PRs para `main` e em pushes para `main`, com Temurin 25 e
  `bash mvnw --batch-mode --no-transfer-progress clean verify`. Confirme os checks do destino real.
- Consulte `target/surefire-reports/` e o resumo do Maven. Investigue falhas e compare com a base
  quando necessário para atribuí-las à mudança. Registre o que ficou sem execução. Um build
  bem-sucedido cobre apenas as verificações que de fato rodaram.
- Para o roteiro HTTP, use a coleção da revisão com o perfil local `setup` e H2 vazio.
  Siga a ordem, use dados fictícios e reinicie a instância isolada para repetir o roteiro.
  O perfil padrão `test` carrega dados de demonstração. Use o Postman CLI se estiver disponível.
  Mudanças específicas de banco externo exigem evidência nesse banco.

## Code Review Rules

Revise o diff e o contexto necessário para confirmar o comportamento: requisitos, chamadores,
contratos, dados persistidos, exceções e testes. Priorize defeitos introduzidos, regressões e lacunas
de teste com consequência demonstrável. Deixe verificações mecânicas para as ferramentas de qualidade.

### Regras de revisão do projeto

- **Isolamento por condomínio.** Nas rotas protegidas, derive o escopo do JWT e confira permissões
  atuais no banco. Restrinja consultas e mutações ao mesmo condomínio. Preserve a negativa padrão
  e as exceções públicas por método e rota. Verifique `401` para token ausente ou inválido e `403`
  para falta de permissão, inclusive para IDs administrativos inexistentes ou de outro condomínio.
- **Acesso do morador.** `PENDING` permite login. Todos os vínculos rejeitados no condomínio
  impedem login. Denunciar exige `APPROVED` no momento do envio, inclusive com token já emitido.
  Cadastros usam o `PasswordEncoder` BCrypt com limite de 72 bytes UTF-8. Guarde `JWT_SECRET` fora
  do repositório e mantenha credenciais, tokens, hashes e CPFs fora de logs e evidências de revisão.
- **Anonimato e protocolo.** A denúncia guarda o condomínio e começa em `RECEIVED`, com UUID v4 único.
  Mantenha conta, vínculo, unidade e origem de rede do denunciante fora da persistência, dos logs
  e das respostas de denúncia. No acompanhamento público, preserve o DTO de campos públicos,
  `404` para protocolo desconhecido e `429` para excesso de consultas. Descrição, local, anexos
  e providências são informações administrativas.
  Em mudanças com imagens, valide o acesso ao anexo pela denúncia e pelo condomínio e confira
  conteúdo, limites e remoção de metadados conforme K01.
- **Cadastros e decisões concorrentes.** Preserve a unicidade de condomínio/síndico por instalação,
  de CPF e de unidade por condomínio, bloco e número, com bloco vazio equivalente ao omitido.
  Grave condomínio/síndico e morador/vínculo atomicamente. Para escolher o primeiro responsável,
  preserve o bloqueio por unidade, a ordem de solicitação com desempate por ID e a unicidade no banco.
  Rejeitar mantém a conta e deixa o próximo pendente elegível para aprovação. Decisões repetidas dão conflito.
- **Status e histórico.** Preserve `RECEIVED → UNDER_REVIEW → IN_PROGRESS → RESOLVED`.
  A transição para `REJECTED` parte de `UNDER_REVIEW` e exige justificativa.
  Preserve a atualização condicional do status anterior e a gravação do histórico na mesma transação.
  Verifique conflito em concorrência, consistência da leitura pública e ordem cronológica com
  desempate por ID. A justificativa é pública. Use providências para informações internas.
- **Contrato e listagem.** Verifique compatibilidade de rotas, métodos, campos, enums, códigos,
  paginação e ordenação. Nos filtros administrativos, preserve o escopo no banco, o limite de
  página, a ordem estável por data e ID e os dias inclusivos em `America/Sao_Paulo`.
  Teste fronteiras de data e filtros combinados quando o período ou a consulta mudar.

Entregue findings por prioridade, com arquivo/linha, condição de ocorrência, evidência, impacto
e correção viável. Use **P0** para impacto crítico imediato, **P1** para defeito grave que bloqueia
integração, **P2** para defeito material de menor alcance e **P3** para problema acionável
não bloqueante. Relate preferências de estilo somente quando gerarem uma consequência concreta.
Se nenhum finding relevante for confirmado, informe isso junto com o escopo e as lacunas de verificação.

### Revisão de Pull Request

Identifique PR, base e origem, commits e SHAs analisados. Consulte descrição, issue, critérios de
aceite, dependências, comentários e checks disponíveis. Use a integração do GitHub e recursos de
revisão do Codex quando estiverem disponíveis e pertinentes. Confirme os achados no diff e nos
consumidores. Durante revisão consultiva, preserve o código do autor e mantenha a avaliação na
conversa. Publicar comentários, aprovar ou solicitar mudanças no GitHub exige autorização.

## PR Trial

1. Confirme a branch de destino e os SHAs atuais de base e origem. Use uma worktree descartável
   ou outra cópia isolada para simular a integração sem alterar a árvore de trabalho do usuário.
2. Registre conflitos e dependências pendentes. Quando houver um candidato integrado, execute
   build e testes pertinentes nele, incluindo regressões de contrato, autorização, persistência
   e configuração conforme o diff. Use banco e credenciais de teste. Se houver mudança de schema
   ou integração externa, valide com dados preexistentes ou serviço isolado, conforme o risco.
3. Identifique as revisões, verificações, resultados, bloqueios e riscos restantes. Indique se há
   evidência de integração segura. Identifique como estática a checagem que não executou a aplicação
   integrada. O trial simula a integração; o merge efetivo depende de autorização explícita.

## Pre-Merge Review

Verifique critérios de aceite, diff final, regressões, compatibilidade com o destino e documentação
afetada. Consulte runs, requisitos de proteção e aprovações do PR para a revisão atual. Confirme
se os resultados do trial e dos testes ainda se aplicam após mudanças na base ou na origem.
O Kanban exige revisão, integração, critérios atendidos e evidências de testes para concluir o cartão.

- **Ready to Merge** — requisitos e verificações obrigatórias atendidos, sem bloqueios identificados.
- **Ready with Observations** — os mesmos requisitos, com observações não bloqueantes.
- **Changes Required** — defeito confirmado, requisito obrigatório descumprido ou check obrigatório com falha relevante.
- **Inconclusive** — falta evidência essencial para decidir.

Informe destino e revisão analisados, evidências, bloqueios e fundamento da conclusão.
Respeite as aprovações humanas, as proteções da branch e os checks exigidos.

## Git e documentação

Preserve alterações preexistentes e confira o diff final, incluindo arquivos novos.
Use a base real do PR. Confirme no repositório a convenção aplicável de branch e commit.
Faça commits, pushes, rebases, merges, publicação em PR ou deploy apenas quando autorizados.
Proteja histórico e dados compartilhados de operações destrutivas.

Atualize contrato, coleção e guias afetados quando a mudança alterar comportamento documentado.
Registre decisões aprovadas junto da documentação correspondente em `docs/`, com links relativos
e exemplos verificáveis. Mantenha aqui instruções duradouras e nos cartões as pendências datadas.
