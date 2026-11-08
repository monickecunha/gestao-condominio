# K04 — Autenticação e autorização

O MVP entrega backend/API de denúncias anônimas; frontend e banco externo continuam opcionais. K04 implementa o login e a política de acesso. As operações de cadastro, vínculos e denúncias são implementadas nos cartões K05–K10/K14; os handlers usados nos testes de segurança existem somente em `src/test`.

## Login e sessão

`POST /api/v1/auth/login` recebe `cpf` e `password`, retornando `token`, `expiresIn: 7200`, `role` (`MANAGER` ou `RESIDENT`) e `condoId`. CPF deve ser enviado no formato armazenado (na carga local, apenas dígitos). Credenciais inválidas retornam 401 sem revelar se o CPF existe. O login nunca retorna a senha ou seu hash.

Conforme K01, tokens são JWT HS256 com `sub`, `role`, `condoId`, `iat` e `exp`, válidos por duas horas. Enviar `Authorization: Bearer <token>`. Não há refresh token. Logout descarta o token no cliente; não há endpoint de logout nem blacklist. Uma cópia do token continua utilizável até expirar, sujeita às permissões atuais no banco. Não são criadas sessões HTTP.

Senhas da carga local são armazenadas com BCrypt, custo 12 e salt individual. Novos serviços de cadastro devem usar o bean `PasswordEncoder.encode` antes de persistir; não passar senha em texto puro ao repositório. BCrypt aceita no máximo 72 bytes UTF-8; o login recusa valores maiores. A decisão K01 não especificava o algoritmo de senha: BCrypt fica registrado como complemento de K04.

Opcionalmente definir `JWT_SECRET` com Base64 de pelo menos 32 bytes aleatórios. A chave não deve ser versionada. Sem essa variável, é gerada uma chave criptograficamente aleatória por inicialização; reiniciar invalida os tokens anteriores. Instâncias que compartilham tokens devem configurar a mesma chave. O console H2 permanece desabilitado e não é uma rota pública.

Um morador com vínculo pendente/rejeitado pode autenticar, mas não denunciar. Sem condomínio associado, o login retorna 403. O campo opcional `condoId` seleciona um condomínio já associado; não concede acesso a outro. Se houver múltiplos condomínios associados, a seleção é obrigatória. Síndico só pode selecionar seu condomínio.

## Política das rotas

| Rota | Acesso |
|---|---|
| POST `/api/v1/auth/login` | Público |
| POST `/api/v1/bindings` | Público para o primeiro cadastro do morador; cria vínculo pendente com a unidade (K06) |
| POST `/api/v1/condominiums` | Cadastro inicial público (K05); instalação já cadastrada retorna 409 |
| GET `/api/v1/complaints/protocol/{protocol}` | Público; a implementação K08 deve retornar apenas campos públicos |
| POST `/api/v1/complaints` | RESIDENT com vínculo APPROVED atual no condomínio do token |
| GET `/api/v1/complaints` | MANAGER; a implementação deve consultar `findAllByCondominiumId` usando o condomínio do token |
| GET `/api/v1/complaints/{id}` | MANAGER do condomínio da denúncia |
| PATCH `/api/v1/complaints/{id}/status` | MANAGER do condomínio da denúncia |
| POST `/api/v1/complaints/{id}/actions` | MANAGER do condomínio da denúncia |
| GET `/api/v1/complaints/{id}/images/{imageId}` | MANAGER; imagem e denúncia devem pertencer ao condomínio |
| GET/POST `/api/v1/condominiums/{condoId}/units` | MANAGER do condomínio indicado |
| GET `/api/v1/bindings` | MANAGER; lista somente pedidos pendentes do condomínio do token |
| POST `/api/v1/bindings/{id}/approve` ou `/reject` | MANAGER do condomínio da unidade vinculada |
| Outras rotas | Negadas por padrão; ampliar explicitamente ao implementar os demais cartões |

Sem autenticação válida: 401. Perfil/condomínio/vínculo sem permissão: 403. Token inválido, adulterado ou expirado: 401. Erros de segurança usam o envelope do contrato HTTP. Recursos de outro condomínio e IDs inexistentes recebem a mesma negativa. As verificações usam o banco em cada requisição. Para listas e criações, os serviços devem derivar o condomínio do JWT, nunca confiar em IDs enviados no corpo ou parâmetros. A denúncia deve continuar sem referência ao usuário/unidade.

## Validação

`SecurityHttpTests` sobe servidor em porta aleatória e usa `java.net.http.HttpClient` com H2 isolado. Cobre login dos dois perfis, credenciais inválidas, hash BCrypt, ausência de token, token inválido/adulterado/expirado, acesso público por protocolo, pendência/rejeição/revogação de vínculo, bloqueio de ações administrativas por morador e isolamento de recursos entre condomínios.

A entidade Condominium do MVP restringe o banco a um condomínio. Somente no schema H2 isolado desta classe de teste, a constraint UNIQUE da coluna singleton é removida antes de inserir o segundo condomínio para validar a defesa de escopo com dois registros reais. A modelagem de produção permanece intacta.

Executar: `.\mvnw.cmd "-Dtest=SecurityHttpTests" test` ou toda a suíte com `.\mvnw.cmd verify` usando JDK 25 ou superior compatível.

