# K15 — Filtros de denúncias

Coleção Postman específica do K15, com a preparação necessária para executar o roteiro sem depender da coleção completa.

Importe [k15-complaint-filters.postman_collection.json](k15-complaint-filters.postman_collection.json) no Postman.

### Preparar o servidor

Use uma versão da API que contenha a implementação K15, como a branch `feature/k15-filtros-denuncias`. Com JDK 25 configurado, execute na raiz do projeto:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=setup
```

No PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=setup"
```

Comece com H2 vazio: se já executou a coleção geral ou outro roteiro no perfil `setup`, reinicie a aplicação. O reinício limpa os dados em memória. Ajuste `baseUrl` na coleção se o servidor não estiver em `http://localhost:8080/api/v1`.

### Executar

Execute a coleção inteira no Collection Runner, com uma iteração, ou envie as requisições em ordem:

| Pasta | Passos | Finalidade |
|---|---|---|
| Preparação | 01–16 | Cadastrar condomínio, síndico, unidade e morador aprovado; autenticar; criar três denúncias e preparar seus status. |
| Filtros e validações | 17–34 | Validar ausência de filtros, categoria, status, período, combinação, paginação, resultados vazios, entradas inválidas e acesso proibido. |
| Isolamento opcional | 35 | Verificar que outro síndico não recebe denúncias do primeiro condomínio. |

Ao terminar a preparação, os dados esperados são:

| Denúncia | Categoria | Status |
|---|---|---|
| Primeira | `NOISE` | `REJECTED` |
| Segunda | `NOISE` | `RESOLVED` |
| Terceira | `SECURITY` | `RECEIVED` |

Tokens, IDs, protocolos e datas são preenchidos automaticamente pelos scripts. O passo 17 calcula as datas a partir dos registros criados no roteiro, usando UTC−03:00 para Brasília. Confira a aba **Test Results**: uma resposta HTTP 200 sozinha não comprova que o filtro funcionou.

Para repetir a coleção inteira, reinicie o perfil `setup` e execute desde o passo 01. Para repetir somente os filtros, execute os passos 17–34 mantendo os mesmos dados e variáveis. Se os tokens expirarem, refaça os logins dos passos 02 e 06. Não repita os cadastros de denúncias na mesma base: os testes esperam exatamente três registros.

### Resultado esperado de cada requisição

Os nomes abaixo são exatamente os exibidos na coleção Postman. Os resultados pressupõem execução em ordem, começando com H2 vazio e sem cadastros extras.

“Primeira”, “segunda” e “terceira denúncia” correspondem aos protocolos `complaintProtocol`, `secondComplaintProtocol` e `k15SecurityProtocol`. Os IDs e protocolos são gerados pelo servidor; não compare com números ou UUIDs fixos.

Nas listagens dos passos 17–27, o padrão é `page: 0` e `size: 20`, exceto nos passos 23 e 24, que usam `size: 1`. Os passos 28–32 devem retornar `status: 400`, `message: "Validation failed"` e `path: "/api/v1/complaints"`, além do erro de campo descrito na tabela. Nesses testes negativos, receber 400, 401 ou 403 é o resultado correto.

#### 1. Preparação

| Nome da requisição no Postman | HTTP esperado | Resultado esperado |
|---|---|---|
| 01. Cadastrar condomínio e síndico | 201 | Condomínio e síndico cadastrados. |
| 02. Fazer login como síndico | 200 | Login do síndico concluído; salva `managerToken` e `condoId`. |
| 03. Criar unidade 101 | 201 | Unidade 101 criada; salva `unitId`. |
| 04. Solicitar vínculo de morador | 201 | Pedido de vínculo de Ana criado; salva `anaBindingId`. |
| 05. Aprovar Ana | 204 | Vínculo de Ana aprovado; resposta sem corpo. |
| 06. Fazer login como morador aprovado | 200 | Login do morador aprovado concluído; salva `residentToken`. |
| 07. Registrar denúncia sem local | 201 | Primeira denúncia criada. Recibo com `message` e `protocol` UUID v4; salva `complaintProtocol`. |
| 08. Registrar outra denúncia e comparar protocolos | 201 | Segunda denúncia criada, com protocolo diferente do primeiro; salva `secondComplaintProtocol`. |
| 09. Conferir primeira página de denúncias | 200 | Página `0`, `size: 1`, `totalElements: 2`, `totalPages: 2`. Retorna apenas a segunda denúncia, ainda `NOISE` / `RECEIVED`; salva `managerComplaintId` e `latestComplaintDate`. |
| 10. Conferir segunda página de denúncias | 200 | Página `1`, `size: 1`, `totalElements: 2`, `totalPages: 2`. Retorna apenas a primeira denúncia, ainda `NOISE` / `RECEIVED`, com data menor ou igual à segunda; salva `firstComplaintId`. |
| 11. Abrir análise da denúncia com local | 200 | Segunda denúncia: `previousStatus: RECEIVED` e `nextStatus: UNDER_REVIEW`. |
| 12. Iniciar providências | 200 | Segunda denúncia: `previousStatus: UNDER_REVIEW` e `nextStatus: IN_PROGRESS`. |
| 13. Resolver a denúncia | 200 | Segunda denúncia: `previousStatus: IN_PROGRESS` e `nextStatus: RESOLVED`. |
| 14. Abrir análise da denúncia sem local | 200 | Primeira denúncia: `previousStatus: RECEIVED` e `nextStatus: UNDER_REVIEW`. |
| 15. Rejeitar com motivo público | 200 | Primeira denúncia: `previousStatus: UNDER_REVIEW` e `nextStatus: REJECTED`, com a justificativa enviada no corpo. |
| 16. registrar denúncia de segurança | 201 | Terceira denúncia criada, com categoria `SECURITY` e status inicial `RECEIVED`; salva `k15SecurityProtocol`. |

#### 2. Filtros e validações

| Nome da requisição no Postman | HTTP esperado | Resultado esperado |
|---|---|---|
| 17. conferir ausência de filtros e preparar datas | 200 | Retorna as três denúncias, nesta ordem: terceira, segunda, primeira. `totalElements: 3`, `totalPages: 1`. Preenche `k15From`, `k15To` e `k15After` para os próximos passos. |
| 18. filtrar somente por categoria | 200 | Retorna somente a terceira denúncia (`SECURITY` / `RECEIVED`). `totalElements: 1`, `totalPages: 1`. |
| 19. filtrar somente por status | 200 | Retorna somente a primeira denúncia (`NOISE` / `REJECTED`). `totalElements: 1`, `totalPages: 1`. Não deve retornar `RECEIVED` nem `RESOLVED`. |
| 20. filtrar somente pela data inicial | 200 | Retorna as três denúncias, na ordem terceira, segunda, primeira: todas estão a partir de `k15From`. `totalElements: 3`, `totalPages: 1`. |
| 21. filtrar somente pela data final | 200 | Retorna as três denúncias, na ordem terceira, segunda, primeira: todas estão até o fim de `k15To`. `totalElements: 3`, `totalPages: 1`. |
| 22. combinar os quatro filtros | 200 | Retorna somente a segunda denúncia (`NOISE` / `RESOLVED`) dentro do período. `totalElements: 1`, `totalPages: 1`. |
| 23. paginar filtros combinados (página 0) | 200 | Página `0`, `size: 1`, `totalElements: 2`, `totalPages: 2`. Retorna somente a segunda denúncia (`NOISE` / `RESOLVED`). |
| 24. paginar filtros combinados (página 1) | 200 | Página `1`, `size: 1`, `totalElements: 2`, `totalPages: 2`. Retorna somente a primeira denúncia (`NOISE` / `REJECTED`). |
| 25. retornar vazio para categoria sem ocorrências | 200 | Página vazia: `content: []`, `totalElements: 0`, `totalPages: 0`. Nenhuma denúncia tem categoria `OTHER`. |
| 26. retornar vazio para combinação incompatível | 200 | Página vazia: `content: []`, `totalElements: 0`, `totalPages: 0`. Nenhuma denúncia combina `SECURITY` e `RESOLVED`. |
| 27. excluir registros anteriores à data inicial | 200 | Página vazia: `content: []`, `totalElements: 0`, `totalPages: 0`. `k15After` é o dia seguinte à data mais recente dos registros. |
| 28. recusar categoria inválida | 400 | `validationErrors` contém `field: category` e `message: Invalid value`, pois `BANANA` não é uma categoria válida. |
| 29. recusar status inválido | 400 | `validationErrors` contém `field: status` e `message: Invalid value`, pois `FINISHED` não é um status válido. |
| 30. recusar data inicial malformada | 400 | `validationErrors` contém `field: from` e `message: Invalid value`, pois `banana` não é uma data válida. |
| 31. recusar data final inexistente | 400 | `validationErrors` contém `field: to` e `message: Invalid value`, pois `2026-02-30` não existe. |
| 32. recusar período invertido | 400 | `validationErrors` contém `field: periodValid` e `message: from must be on or before to`, pois a data inicial é posterior à final. |
| 33. recusar filtros para morador | 403 | Acesso negado ao morador, mesmo com token válido; não retorna a listagem. |
| 34. exigir login para filtrar | 401 | Acesso negado por ausência de token; não retorna a listagem. |

#### 3. Isolamento opcional

| Nome da requisição no Postman | HTTP esperado | Resultado esperado |
|---|---|---|
| 35. opcional: preservar isolamento com filtros | 200 ou ignorado | Sem `otherManagerToken`, a requisição é ignorada e não comprova isolamento. Com a fixture e token válidos, retorna somente denúncias `NOISE` do outro condomínio no período, sem nenhum dos três protocolos desta preparação. `content.length` deve ser igual a `totalElements` (fixture com até 100 resultados). |

### Limites do roteiro

- O passo 35 é ignorado quando `otherManagerToken` está vazio. Para executá-lo, é necessário preparar outro condomínio no mesmo servidor, com token válido e até 100 denúncias correspondentes aos filtros. O perfil padrão admite um único condomínio; a coleção não altera essa regra.
- O isolamento com dois condomínios e a virada do dia no fuso de Brasília são cobertos pelos testes automatizados `ComplaintFilterHttpTests`. Esta coleção usa os horários reais de criação e não modifica datas diretamente no banco.
- A coleção geral permanece disponível em [condominium-management.postman_collection.json](../../condominium-management.postman_collection.json).
