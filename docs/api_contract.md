# Contrato da API REST — Gestão Condominial (K11)

Este documento define os principais endpoints, payloads (DTOs) e retornos da API.
**URL Base:** `/api/v1`

## Teste local no Postman

1. Na raiz do projeto, com JDK 25 configurado, execute `./mvnw spring-boot:run -Dspring-boot.run.profiles=setup`. No PowerShell, use `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=setup"`. Espere a aplicação iniciar e mantenha o terminal aberto. Esse perfil usa H2 vazio; reiniciar a aplicação limpa os dados.
2. No Postman, clique em **Import** e selecione a [coleção de exemplos](gestao-condominio.postman_collection.json) da pasta `docs`. A variável da coleção `baseUrl` começa em `http://localhost:8080/api/v1`. Se iniciou em outra porta, ajuste essa variável.
3. Abra as requisições na ordem numérica e clique em **Send** em cada uma, ou use o executor da coleção. O login do síndico salva o token e o ID do condomínio; o cadastro da unidade salva seu ID. As chamadas seguintes usam esses valores automaticamente.
4. Confira a sequência: cadastro inicial (201), login do síndico (200), lista vazia (200), criação da unidade (201), lista com a unidade (200), fila vazia (200), pedidos de Ana e Bruno (201), fila com ambos (200), login do morador (200) e repetição do pedido (409). As requisições 12 a 16 verificam autoaprovação (403), prioridade de Ana (409 ao tentar Bruno), aprovação de Ana (204), fila somente com Bruno (200) e aprovação repetida (409). As requisições 17 a 31 exercitam a rejeição em uma segunda unidade. As requisições 32 a 37 testam o registro de denúncias com o token de Ana da unidade 101, já aprovada na requisição 14. Os passos 38 a 40 consultam o protocolo, 41 a 52 conferem as consultas administrativas e 53 a 67 validam a atualização de status, as providências e o histórico público. O passo 68 testa o limite de consultas públicas e deve ser executado por último. Para repetir desde o primeiro passo, reinicie o perfil `setup`.

## Endpoints disponíveis nesta versão

Todos os caminhos abaixo usam a URL base. Nas requisições com corpo, envie `Content-Type: application/json`. No Postman, use **Body → raw → JSON**. Para as operações do síndico, selecione **Authorization → Bearer Token** e cole o token retornado pelo login.

| Método e caminho | Autorização | Dados necessários | Sucesso |
|---|---|---|---|
| `POST /condominiums` | No Auth | Corpo com `condominium.name`, `manager.name`, `manager.cpf` e `manager.password`. Opcionais na seção de cadastro abaixo. | 201 com `condoId` e `managerId` |
| `POST /auth/login` | No Auth | Corpo com `cpf` e `password`; inclua `condoId` se houver mais de um condomínio elegível para login. | 200 com `token`, `expiresIn`, `role` e `condoId` |
| `POST /condominiums/{condoId}/units` | Bearer do síndico | `condoId` do login na URL. Corpo com `number` obrigatório e `block` opcional. | 201 com `id`, `number` e `block` |
| `GET /condominiums/{condoId}/units` | Bearer do síndico | `condoId` do login na URL. Sem corpo. | 200 com lista, inclusive `[]` |
| `POST /bindings` | No Auth | Corpo com `name`, `cpf`, `password` e `unitId` (o `id` retornado ao criar a unidade). | 201 com `bindingId`, `unitId` e `status: "PENDING"` |
| `GET /bindings` | Bearer do síndico | Sem corpo. Parâmetro opcional `status=PENDING`. | 200 com os pedidos pendentes do condomínio, inclusive `[]` |
| `POST /bindings/{bindingId}/approve` | Bearer do síndico | ID do pedido na URL. Sem corpo. | 204 sem corpo |
| `POST /bindings/{bindingId}/reject` | Bearer do síndico | ID do pedido na URL. Sem corpo. | 204 sem corpo |
| `POST /complaints` | Bearer de morador aprovado | Corpo com `category` e `description`; `location` opcional. | 201 com protocolo e mensagem |
| `GET /complaints/protocol/{protocol}` | Público | Protocolo retornado no cadastro | 200 com status e histórico público |
| `GET /complaints` | Síndico | `page` e `size` opcionais | 200 com página do próprio condomínio |
| `GET /complaints/{id}` | Síndico | ID da denúncia | 200 com detalhes e providências internas |
| `PATCH /complaints/{id}/status` | Síndico | `newStatus` e justificativa obrigatória na rejeição | 200 com a transição registrada |
| `POST /complaints/{id}/actions` | Síndico | `actionDescription` | 201 com a providência registrada |

---

## 1. Padrão de Erros (Error Handling)
Todas as respostas de erro (400, 401, 403, 404, 500) seguirão este formato padrão:

```json
{
  "timestamp": "2026-11-02T15:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/complaints",
  "validationErrors": [
    {
      "field": "description",
      "message": "Description is required"
    }
  ]
}
```

---

## 2. Autenticação (Auth)

### `POST /auth/login`
Autentica um usuário (Síndico ou Morador) e retorna o token JWT. Para o morador, vínculos rejeitados são desconsiderados na seleção do condomínio, inclusive quando o pedido informa `condoId`. Se todos os vínculos no condomínio estiverem rejeitados, o login com credenciais válidas retorna `403` sem token.
*   **Request Body:**
    ```json
    {
      "cpf": "11122233344",
      "password": "password123"
    }
    ```
*   **Response (200 OK):**
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5...",
      "expiresIn": 7200,
      "role": "MANAGER",
      "condoId": 1
    }
    ```

---

## 3. Gestão de Condomínio e Unidades (Manager)

### `POST /condominiums` — Cadastro inicial (K05)

Cadastra o único condomínio da instalação e seu síndico em uma transação. Esta rota
aceita requisições sem token para permitir a criação da primeira conta. No ambiente
de demonstração, faça o cadastro localmente antes de expor a instalação a outros clientes.

Campos obrigatórios: `condominium.name`, `manager.name` (até 150 caracteres cada),
`manager.cpf` (até 14 caracteres, no formato que será usado no login) e
`manager.password` (até 72 bytes em UTF-8). Campos opcionais: `condominium.cnpj`
(até 18 caracteres), `condominium.address` (255) e `phone` em ambos os objetos (20).

Para testar com H2 **vazio**, inicie a aplicação com o perfil `setup`:
`./mvnw spring-boot:run -Dspring-boot.run.profiles=setup`. O perfil padrão
`test` já contém um condomínio. Os dados do H2 em memória duram até encerrar a aplicação.

1. Cadastrar em um terminal:

   ```bash
   curl -i http://localhost:8080/api/v1/condominiums \
     -H 'Content-Type: application/json' \
     -d '{"condominium":{"name":"Residencial Parque"},"manager":{"name":"Maria","cpf":"12345678901","password":"teste-local"}}'
   ```

   **201 Created**: `{"condoId":1,"managerId":1}` (IDs ilustrativos).

2. Fazer login com o síndico recém-criado:

   ```bash
   curl -i http://localhost:8080/api/v1/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"cpf":"12345678901","password":"teste-local"}'
   ```

   **200 OK**: a resposta traz o token, o perfil `MANAGER` e o mesmo `condoId`.

3. Repetir o primeiro comando retorna **409 Conflict**. CPF já cadastrado
   também resulta em 409. Em caso de falha, nenhum registro parcial é salvo.
   Campos inválidos e JSON malformado retornam **400 Bad Request** no envelope
   da seção 1. Para erros de campos, `validationErrors` indica o caminho, por
   exemplo `manager.password`, e a mensagem de validação.

### Unidades — `/condominiums/{condoId}/units` (K05)

O síndico envia o token no cabeçalho `Authorization: Bearer <token>`. Use na
URL o `condoId` devolvido no login. As duas operações retornam 401 sem token
válido e 403 para um morador ou para um síndico que informe outro condomínio.

| Método | Requisição | Resposta |
|---|---|---|
| POST | `{"number":"101"}` ou `{"number":"101","block":"A"}` | 201 com a unidade criada, por exemplo `{"id":1,"number":"101","block":""}`. |
| GET | Sem corpo | 200 com as unidades do condomínio em ordem de ID. Para um condomínio sem unidades, retorna `[]`. |

O número é texto obrigatório e o bloco é opcional, ambos com até 20 caracteres.
Ao omitir o bloco, a API devolve `""`. Repetir número e bloco no mesmo condomínio
retorna 409; o mesmo número pode existir em blocos diferentes. Dados inválidos
retornam 400 com o envelope da seção 1.

Para reproduzir depois do login, defina `CONDO_ID` e `TOKEN` com os valores da resposta:

```bash
curl -i -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/condominiums/$CONDO_ID/units"

curl -i -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"number":"101"}' "http://localhost:8080/api/v1/condominiums/$CONDO_ID/units"
```

### `POST /bindings` — Primeiro cadastro do morador (K06)

Rota pública para uma **nova conta**. O campo `unitId` é o ID da unidade
criada pelo síndico na K05. O condomínio vem dessa unidade. Nome (até 150
caracteres), CPF (até 14), senha (até 72 bytes em UTF-8) e ID positivo são
obrigatórios. Use o mesmo formato de CPF no login.

```bash
curl -i -H 'Content-Type: application/json' \
  -d '{"name":"Ana","cpf":"44455566677","password":"teste-morador","unitId":1}' \
  http://localhost:8080/api/v1/bindings
```

Substitua `1` pelo ID real. A API grava a conta e o vínculo na mesma transação.
No sucesso, responde `201 Created`, por exemplo
`{"bindingId":7,"unitId":1,"status":"PENDING"}`. O vínculo começa pendente
e o morador ainda não é responsável pela unidade. O login da K04 já retorna
`RESIDENT` e o condomínio da unidade; registrar denúncia exige vínculo aprovado.

| Situação | Resposta |
|---|---|
| Campo obrigatório ausente ou inválido | 400 no envelope de validação |
| Unidade inexistente | 404; a conta e o vínculo não são criados |
| CPF já cadastrado, inclusive ao repetir o pedido | 409; senha e vínculos existentes são preservados |

### `GET /bindings` — Pedidos pendentes do condomínio (K06)

Use o token do síndico. O condomínio vem do token. A consulta sem parâmetros e a
consulta com `?status=PENDING` retornam a mesma fila, da solicitação mais antiga
à mais recente. Em caso de empate, o menor `bindingId` vem primeiro.

```bash
curl -i -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/v1/bindings?status=PENDING'
```

Exemplo de resposta 200:

```json
[{"bindingId":7,"residentName":"Ana","unitId":1,"number":"101","block":"","status":"PENDING","requestedAt":"2026-11-08T14:00:00Z"}]
```

Sem pendências, o resultado é `[]`. Um filtro como `status=APPROVED` retorna
400 e indica `status` em `validationErrors`. Sem token válido, o retorno é
401; com token de morador, 403.

### `POST /bindings/{bindingId}/approve` — Aprovar pedido

Use o `bindingId` da fila e o token do síndico do condomínio. Envie um POST sem corpo.
Em uma unidade sem responsável, o pedido pendente mais antigo tem prioridade (desempate por ID).
Ao ser aprovado, esse morador se torna responsável. Se a unidade já tem responsável,
aprovar outro pedido pendente mantém o responsável atual.

```bash
curl -i -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/bindings/$BINDING_ID/approve"
```

| Resposta | Quando ocorre |
|---|---|
| 204 sem corpo | Vínculo aprovado e retirado da fila. |
| 401 | Token ausente ou inválido. |
| 403 | Morador, pedido de outro condomínio ou ID inexistente. |
| 409 | Pedido fora de ordem para a escolha do responsável, ou vínculo que já saiu de `PENDING`. |

### `POST /bindings/{bindingId}/reject` — Rejeitar pedido

Use o ID do vínculo no caminho e o token do síndico do condomínio. O pedido
deve estar pendente. A operação retira o vínculo da fila, preserva a conta do
morador e deixa o próximo pendente elegível para responsável se a unidade ainda
estiver sem responsável. Se esse era o único vínculo do morador no condomínio, uma nova tentativa de login retorna `403`. Registrar denúncia com um token anterior à rejeição também exige ao menos um vínculo aprovado no condomínio do token.

```bash
curl -i -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/bindings/$BINDING_ID/reject"
```

| Código | Resultado |
|---|---|
| 204 | Vínculo alterado para `REJECTED`; resposta sem corpo. |
| 401 | Sem token válido. |
| 403 | Morador, pedido de outro condomínio ou ID inexistente. |
| 409 | O vínculo já recebeu uma decisão ou está fora do estado `PENDING`. |

---

## 4. Denúncias

### `POST /complaints`
Registra uma denúncia no condomínio informado pelo token. Requer morador com vínculo aprovado no momento do envio. A descrição é obrigatória e deve conter texto; o local é opcional (até 255 caracteres). Categorias aceitas: `NOISE`, `SECURITY`, `COMMON_AREA_MISUSE`, `GARBAGE_VIOLATION`, `RULES_VIOLATION` e `OTHER`.

A denúncia começa em `RECEIVED`. O registro guarda o condomínio e os dados da ocorrência, sem identificador do morador, unidade ou vínculo. A resposta contém um protocolo UUID v4 único e uma mensagem. Guarde o protocolo para o acompanhamento público. Erros de entrada retornam 400, token ausente ou inválido retorna 401 e perfil ou vínculo sem permissão retorna 403.

*   **Request Body:**
    ```json
    {
      "category": "NOISE",
      "description": "Barulho excessivo de furadeira após as 22h.",
      "location": "Corredor do 5º andar, bloco B"
    }
    ```
*   **Response (201 Created):** Retorna o recibo após a gravação.
    ```json
    {
      "protocol": "550e8400-e29b-41d4-a716-446655440000",
      "message": "Complaint registered successfully."
    }
    ```

### `GET /complaints/protocol/{protocol}`
Consulta pública do andamento da denúncia pelo protocolo (Não requer autenticação).
*   **Response (200 OK):** *Apenas campos públicos.*
    ```json
    {
      "protocol": "550e8400-e29b-41d4-a716-446655440000",
      "category": "NOISE",
      "status": "UNDER_REVIEW",
      "createdAt": "2026-11-02T10:00:00Z",
      "updatedAt": "2026-11-02T14:00:00Z",
      "statusHistory": [
        {
          "previousStatus": "RECEIVED",
          "nextStatus": "UNDER_REVIEW",
          "changedAt": "2026-11-02T14:00:00Z",
          "publicJustification": null
        }
      ]
    }
    ```

### `GET /complaints/{id}`
Consulta interna detalhada da denúncia. (Requer perfil `MANAGER`).
*   **Response (200 OK):** *Inclui dados completos.*
    ```json
    {
      "id": 15,
      "protocol": "550e8400-e29b-41d4-a716-446655440000",
      "category": "NOISE",
      "status": "UNDER_REVIEW",
      "createdAt": "2026-11-02T10:00:00Z",
      "fullDescription": "Barulho excessivo de furadeira após as 22h.",
      "location": "Corredor do 5º andar, bloco B",
      "internalActions": [
        {
          "actionDescription": "Notificação enviada ao morador do 501.",
          "executedAt": "2026-11-02T15:00:00Z",
          "registeredBy": "Síndico João"
        }
      ]
    }
    ```

### `PATCH /complaints/{id}/status`
Atualiza o status de uma denúncia. (Requer perfil `MANAGER`).
*   **Request Body:**
    ```json
    {
      "newStatus": "REJECTED",
      "justification": "Não foram encontradas evidências de barulho no horário citado."
    }
    ```
O fluxo permitido é `RECEIVED → UNDER_REVIEW → IN_PROGRESS → RESOLVED`. A transição para `REJECTED` só pode partir de `UNDER_REVIEW`. Uma transição inválida retorna 409. Status ausente ou desconhecido e rejeição sem justificativa preenchida retornam 400.

Em caso de sucesso (200), a API retorna o item gravado no histórico. O campo `justification`, quando informado, é público e aparece no acompanhamento por protocolo. Use o endpoint de providências para registrar observações internas.

```json
{
  "previousStatus": "UNDER_REVIEW",
  "nextStatus": "REJECTED",
  "changedAt": "2026-11-02T15:00:00Z",
  "publicJustification": "Não foram encontradas evidências de barulho no horário citado."
}
```

### `POST /complaints/{id}/actions`

Envie `{"actionDescription":"Vistoria no corredor"}`. A descrição é obrigatória e deve conter texto. O servidor registra a data e identifica o responsável pelo token. A resposta 201 contém `actionDescription`, `executedAt` e `registeredBy`. O detalhe administrativo devolve essas providências em `internalActions`, em ordem de registro.

Ambas as operações exigem token de síndico do condomínio da denúncia. Token ausente ou inválido retorna 401. Morador e síndico de outro condomínio recebem 403.

## Complemento K04

A implementação de autenticação e a matriz de autorização estão em [K04](k04_authentication_authorization.md). O login aceita adicionalmente `condoId` opcional para selecionar um condomínio elegível para login (obrigatório se houver mais de um). Sem associação válida retorna 403; credenciais inválidas retornam 401. Token expirado/inválido retorna 401; perfil, condomínio ou vínculo sem permissão retorna 403. Logout consiste em descartar o token no cliente.
