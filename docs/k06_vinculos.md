# K06 — Cadastro, consulta e aprovação de vínculos

A primeira entrega da [K06](https://github.com/monickecunha/gestao-condominio/issues/6) permite que um novo morador crie sua conta e solicite vínculo com uma unidade existente. O pedido nasce `PENDING`. Com essa conta, o morador já consegue fazer login; a autorização para denunciar depende da aprovação do vínculo pelo síndico.

## Como o pedido é registrado

1. O síndico cadastra a unidade e a API devolve seu `id`. Esse é o `unitId` usado no pedido. Por exemplo, se a unidade 101 tem `id: 3`, o morador envia `unitId: 3`. Veja o fluxo anterior na [K05](k05_cadastro_inicial.md).
2. O morador envia `POST /api/v1/bindings` sem token, pois está criando sua primeira conta. O `BindingController` recebe o `BindingRequest` com quatro campos obrigatórios: `name` (até 150 caracteres), `cpf` (até 14), `password` (até 72 bytes em UTF-8) e `unitId` (inteiro positivo). O condomínio é determinado pela unidade cadastrada.
3. O `BindingService` localiza a unidade, protege a senha com o mesmo BCrypt usado pela K04 e grava a conta e o vínculo em uma única transação. O servidor define o status inicial. A resposta `201 Created` contém `bindingId`, `unitId` e `status: "PENDING"`. Há um exemplo executável no [contrato HTTP](api_contract.md).

## Regras verificadas

| Situação | Comportamento |
|---|---|
| Dois moradores com CPFs diferentes solicitam a mesma unidade | Cada conta recebe um `bindingId` próprio. Os dois pedidos têm o mesmo `unitId`, ficam pendentes e nenhum solicitante se torna responsável nesse momento. |
| Um cadastro é reenviado com CPF já utilizado | A API devolve `409 Conflict`. A senha original continua válida e os vínculos existentes são preservados. |
| A unidade informada não existe | A API devolve `404 Not Found`. Como a unidade é consultada antes da gravação, é possível corrigir o ID e reenviar o pedido com o mesmo CPF. |
| Faltam campos ou a senha excede o limite em bytes | A API devolve `400 Bad Request`. O cliente encontra os campos a corrigir em `validationErrors`. |
| O morador pendente faz login e tenta denunciar | O login retorna `200`, token, perfil `RESIDENT` e o condomínio da unidade. Com esse token, `POST /api/v1/complaints` retorna `403`, conforme a [política de acesso da K04](k04_authentication_authorization.md). |

## Validação do cadastro do morador

No Postman, o fluxo foi validado desde o cadastro do condomínio e do síndico até a criação da unidade e os pedidos de Ana e Bruno. A comparação das duas respostas confirmou vínculos distintos para a mesma unidade. O roteiro incluiu a repetição do cadastro com outra senha, o login com a senha original, o bloqueio da denúncia pendente e o reenvio após corrigir uma unidade inexistente.

Para repetir os quatro testes HTTP específicos com H2, execute `./mvnw -Dtest=BindingRegistrationHttpTests test`. Eles cobrem criação e login com vínculo pendente, unidade inexistente sem conta parcial, duplicidade com preservação da senha e validação da entrada. Na primeira etapa, a suíte completa passou com 33 testes, sem falhas ou erros. [Testes de cadastro e vínculo](../src/test/java/residencia/gestao_condominio/controllers/BindingRegistrationHttpTests.java)

## Consulta de pedidos pendentes pelo síndico

O síndico envia `GET /api/v1/bindings` com o token obtido no login. O condomínio consultado vem do token. Omitir o filtro ou enviar `?status=PENDING` retorna a mesma fila, ordenada do pedido mais antigo ao mais recente. Em caso de empate na data, o menor `bindingId` aparece primeiro.

Cada item informa `bindingId`, `residentName`, `unitId`, `number`, `block`, `status` e `requestedAt`. Assim, o síndico consegue distinguir duas solicitações da mesma unidade pelo identificador do vínculo. O [contrato da API](api_contract.md) contém um exemplo completo de resposta.

### Validação no Postman

Em 08/11/2026, a equipe validou o fluxo com H2 em memória. Para reproduzir, inicie a aplicação com o perfil `setup` e execute as requisições da [coleção do Postman](gestao-condominio.postman_collection.json) em ordem.

| Requisição | Resultado observado |
|---|---|
| 6 — Consultar a fila antes dos pedidos | O token do síndico permitiu a consulta, que retornou `200 OK` com `[]`. |
| 7 e 8 — Cadastrar Ana e Bruno na unidade 101 | Cada cadastro retornou `201 Created` com o mesmo `unitId`, um `bindingId` distinto e status `PENDING`. |
| 9 — Consultar a fila após os cadastros | O síndico recebeu `200 OK` com Ana primeiro e Bruno em seguida, na ordem das solicitações. |

### Cobertura automatizada da listagem

Os testes HTTP com H2 verificam que a fila contém apenas pedidos pendentes do condomínio do síndico, inclusive quando existem registros de outro condomínio. Também verificam lista vazia, ordenação, exclusão de vínculos aprovados ou rejeitados e respostas de erro: `400` para filtro inválido, `401` sem autenticação e `403` com token de morador. Para repetir esses cenários, execute `./mvnw -Dtest=SecurityHttpTests test`. A suíte completa da segunda etapa passou em `./mvnw clean verify` com **35 testes, zero falhas e zero erros**. [Testes HTTP de segurança](../src/test/java/residencia/gestao_condominio/security/SecurityHttpTests.java)

## Aprovação de um pedido

O síndico escolhe um `bindingId` da fila do próprio condomínio e envia
`POST /api/v1/bindings/{bindingId}/approve` com seu token, sem corpo. A aprovação
muda o vínculo de `PENDING` para `APPROVED` e retorna `204 No Content`. A K04
consulta as permissões atuais no banco, então o morador pode usar o token que
já tinha para operações que exigem vínculo aprovado.

Se a unidade ainda não tem responsável, a prioridade é do primeiro pedido
pendente da **unidade**, seguindo a mesma ordem da listagem. Por exemplo, se
Ana pediu antes de Bruno, tentar aprovar Bruno primeiro retorna `409` e ambos
continuam pendentes. Ao aprovar Ana, ela se torna responsável e somente Bruno
permanece na fila. Aprovar outro vínculo depois disso preserva a indicação de Ana.

O serviço decide e grava a mudança na mesma transação, com bloqueio da unidade.
Isso permite serializar cadastros e aprovações concorrentes naquela unidade.
Dois envios simultâneos para aprovar o mesmo pedido resultam em uma aprovação e
um conflito. O morador não pode se autoaprovar; o síndico de outro condomínio
recebe a mesma negativa que receberia para um ID inexistente.

Para validar manualmente, importe a [coleção atualizada](gestao-condominio.postman_collection.json)
e execute em ordem com o banco vazio. As requisições 7 e 8 guardam os IDs de Ana e Bruno;
as requisições 12 a 16 testam permissões, prioridade, aprovação e a fila resultante.

## Continuidade do cartão

O próximo bloco é a rejeição de um pedido pelo síndico. Quando um pedido sair da fila por rejeição, o próximo pendente poderá ser considerado para responsável de uma unidade ainda sem responsável. O convite de moradores adicionais permanece na K16. [Decisões da K01](k01_technical_decisions_summary.md)
