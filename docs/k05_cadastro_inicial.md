# K05 — Cadastro inicial do condomínio e do síndico

A primeira parte da [K05](https://github.com/monickecunha/gestao-condominio/issues/5) atende à história US01. Em uma instalação vazia, o síndico cadastra seu condomínio e cria a conta que usará para administrá-lo. O próximo bloco da K05 é o cadastro e a listagem de unidades pelo síndico autenticado.

## Como criar a primeira conta

Como o primeiro síndico ainda não tem credenciais, a configuração de segurança permite chamar `POST /api/v1/condominiums` sem token. Quem faz o primeiro cadastro válido passa a administrar a instalação. Por isso, na demonstração, o cadastro deve ser feito localmente antes de disponibilizar a aplicação a outros clientes.

A requisição reúne os dados de `condominium` e `manager`. No sucesso, a API devolve `201 Created` com `condoId` e `managerId`. Para obter o token, o síndico usa o login da K04 com o mesmo CPF e senha. O token identifica o perfil `MANAGER` e o condomínio criado. Os campos obrigatórios, os limites de tamanho e os exemplos de requisição estão no [contrato HTTP](api_contract.md).

## Por que as duas gravações ficam na mesma transação

O MVP admite um condomínio e um síndico por instalação. O método `register` de `CondominiumRegistrationService` usa `@Transactional` para gravar primeiro o condomínio e depois o administrador associado a ele. Se o CPF já estiver em uso, a gravação do administrador falha e a transação desfaz a inserção do condomínio. A instalação continua disponível para uma nova tentativa válida.

A restrição de unicidade da coluna `singleton` garante essa regra inclusive quando dois cadastros chegam ao mesmo tempo. O banco aceita uma das transações, e a outra requisição recebe `409 Conflict`. A associação única e obrigatória entre administrador e condomínio já faz parte do [modelo da K03](diagrama_er_condominio.md).

## Responsabilidades e respostas de erro

- **Controller e DTOs.** O `CondominiumController` recebe a requisição e valida os dois objetos antes de chamar o serviço. Os DTOs são `record` porque os campos de entrada e saída são definidos pelo contrato. A resposta contém somente os dois identificadores.
- **Serviço e senha.** O serviço coordena os repositórios e usa o mesmo `PasswordEncoder` BCrypt que o login usa para verificar a senha. A validação do cadastro mede o limite de 72 **bytes em UTF-8**, pois caracteres acentuados podem ocupar mais de um byte. Veja a [decisão de autenticação da K04](k04_authentication_authorization.md).
- **Erros.** O `CondominiumRegistrationErrors` traduz exceções de validação e persistência para o formato de erro da API. JSON malformado ou campo inválido resulta em `400`; no segundo caso, `validationErrors` identifica o campo. Uma violação de unicidade, como instalação já cadastrada ou CPF em uso, resulta em `409`. As exceções do framework já distinguem os casos tratados nesta etapa.

## Como verificar

Para testar manualmente, inicie com `./mvnw spring-boot:run -Dspring-boot.run.profiles=setup` e execute os exemplos de cadastro e login do [contrato HTTP](api_contract.md). O perfil `setup` inicia com H2 vazio. O perfil padrão `test` carrega um condomínio de demonstração, por isso já permite verificar o conflito de uma segunda instalação.

Com o JDK do projeto, execute `./mvnw -Dtest=CondominiumRegistrationHttpTests test`. Os cinco testes exercitam a API por HTTP e conferem o resultado no H2:

| Cenário | Resultado verificado |
|---|---|
| Cadastro válido seguido de login | Cadastro retorna 201; o login retorna 200, token, perfil MANAGER e o mesmo condomínio. |
| Campos inválidos ou JSON malformado | A API retorna 400 e nenhuma conta ou condomínio é criado. |
| Segundo cadastro com outros dados | A API retorna 409 e preserva o primeiro cadastro. |
| Falha ao gravar o síndico por CPF em uso | A API retorna 409, preserva o usuário anterior e desfaz a criação do condomínio. |
| Duas requisições simultâneas | Uma retorna 201 e a outra 409; resta exatamente um condomínio com um síndico. |
