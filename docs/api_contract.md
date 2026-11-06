# Contrato da API REST — Gestão Condominial (K11)

Este documento define os principais endpoints, payloads (DTOs) e retornos da API.
**URL Base:** `/api/v1`

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
Autentica um usuário (Síndico ou Morador) e retorna o token JWT.
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

### `POST /condominiums/{condoId}/units`
Cria uma nova unidade no condomínio. (Requer perfil `MANAGER`).
*   **Request Body:**
    ```json
    {
      "block": "A",
      "number": "101"
    }
    ```
*   **Response (201 Created):** Retorna os dados da unidade criada com seu `id`.

### `POST /bindings/{id}/approve`
Aprova a solicitação de vínculo de um morador a uma unidade. (Requer perfil `MANAGER`).
*   **Response (204 No Content):** Sem corpo, apenas status de sucesso.

---

## 4. Denúncias (Complaints)

### `POST /complaints`
Registra uma nova denúncia anônima. (Requer perfil `RESIDENT` com vínculo aprovado).
*   **Request Body:**
    ```json
    {
      "category": "NOISE",
      "description": "Barulho excessivo de furadeira após as 22h.",
      "location": "Corredor do 5º andar, bloco B"
    }
    ```
*   **Response (201 Created):** Retorna apenas o protocolo gerado.
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
      "attachments": [
        { "imageId": 42, "url": "/api/v1/complaints/15/images/42" }
      ],
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
*   **Response (200 OK):** Status atualizado.
### Complemento K04

A implementação de autenticação e a matriz de autorização estão em [K04](k04_authentication_authorization.md). O login aceita adicionalmente `condoId` opcional para selecionar um condomínio já associado (obrigatório se houver mais de um). Sem associação válida retorna 403; credenciais inválidas retornam 401. Token expirado/inválido retorna 401; perfil, condomínio ou vínculo sem permissão retorna 403. Logout consiste em descartar o token no cliente.
