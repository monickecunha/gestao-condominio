# K13 — Fluxo administrativo integrado

Este roteiro demonstra o ciclo completo sem frontend: o morador registra a denúncia, o síndico trata e o morador acompanha pelo protocolo.

## Testes automatizados

A classe `ComplaintLifecycleHttpTests` sobe a API com H2 em memória e faz requisições HTTP reais. Ela cobre:

| Cenário | Resultado esperado |
|---|---|
| Ciclo completo: denúncia, listagem, detalhe, análise, providência, execução, resolução e acompanhamento público | Histórico público com 3 transições; providências e descrição não aparecem na consulta por protocolo |
| Rejeição sem justificativa ou com justificativa em branco | 400 |
| Rejeição justificada | 200; justificativa visível no acompanhamento |
| Transições fora do fluxo (`RECEIVED → RESOLVED`, `RECEIVED → REJECTED`, `RESOLVED → IN_PROGRESS`) | 409, status mantido |
| Status desconhecido, status ausente ou providência em branco | 400 |
| Síndico de outro condomínio | 403 no detalhe, no status e nas providências; denúncia ausente da lista dele |
| Morador / sem token | 403 / 401 |

Para executar somente esta classe:

```powershell
.\mvnw.cmd "-Dtest=ComplaintLifecycleHttpTests" test
```

## Roteiro HTTP manual

Inicie a aplicação com o perfil `setup` (veja o [contrato da API](api_contract.md)). Na [coleção do Postman](gestao-condominio.postman_collection.json), execute os passos 1 a 37 para criar condomínio, unidade, morador aprovado e denúncia. Em seguida:

| Passo | Requisição | Token | Esperado |
|---|---|---|---|
| 1 | `GET /complaints?status=RECEIVED` | Síndico | 200; anote o `id` e o `protocol` |
| 2 | `GET /complaints/{id}` | Síndico | 200 com descrição, local e `internalActions: []` |
| 3 | `PATCH /complaints/{id}/status` com `{"newStatus":"UNDER_REVIEW"}` | Síndico | 200 |
| 4 | `POST /complaints/{id}/actions` com `{"actionDescription":"Vistoria no corredor"}` | Síndico | 201 |
| 5 | `PATCH /complaints/{id}/status` com `{"newStatus":"RESOLVED"}` | Síndico | 409 (pula etapa) |
| 6 | `PATCH /complaints/{id}/status` com `{"newStatus":"IN_PROGRESS"}` | Síndico | 200 |
| 7 | `PATCH /complaints/{id}/status` com `{"newStatus":"RESOLVED","justification":"Morador notificado."}` | Síndico | 200 |
| 8 | `GET /complaints/{id}` | Síndico | 200, `status: RESOLVED` e uma providência |
| 9 | `GET /complaints?status=RESOLVED` | Síndico | 200, denúncia na lista |
| 10 | `GET /complaints/protocol/{protocol}` | Nenhum | 200, três itens em `statusHistory`, sem providências |

Os passos 38 a 88 da coleção já automatizam essas chamadas, inclusive os filtros do K15. Imagens (K14) serão incluídas quando o cartão estiver disponível.