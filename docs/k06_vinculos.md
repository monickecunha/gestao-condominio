# K06 — Cadastro do morador e solicitação de vínculo

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

## Evidências de teste

No Postman, o fluxo foi validado desde o cadastro do condomínio e do síndico até a criação da unidade e os pedidos de Ana e Bruno. A comparação das duas respostas confirmou vínculos distintos para a mesma unidade. O roteiro incluiu a repetição do cadastro com outra senha, o login com a senha original, o bloqueio da denúncia pendente e o reenvio após corrigir uma unidade inexistente.

Para repetir os quatro testes HTTP específicos com H2, execute `./mvnw -Dtest=BindingRegistrationHttpTests test`. Eles cobrem criação e login com vínculo pendente, unidade inexistente sem conta parcial, duplicidade com preservação da senha e validação da entrada. A suíte completa desta entrega passou com 33 testes, sem falhas ou erros. [Testes de cadastro e vínculo](../src/test/java/residencia/gestao_condominio/controllers/BindingRegistrationHttpTests.java)

## Continuidade do cartão

Na próxima etapa da K06, o síndico deverá listar os pedidos do próprio condomínio e aprovar ou rejeitar cada vínculo. A aprovação deverá aplicar a regra de escolha do responsável pela unidade e os testes deverão conferir a proteção contra autoaprovação e decisões de outro condomínio. O convite de moradores adicionais pelo responsável aprovado pertence à K16. [Decisões da K01](k01_technical_decisions_summary.md)
