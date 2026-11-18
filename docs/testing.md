# Gestão de Condomínio: Testes

Execute os comandos na pasta raiz do projeto, onde está o `pom.xml`. Utilize **JDK 25** e configure `JAVA_HOME`. O Maven Wrapper já está incluído no repositório.

## Linux e macOS

Confira o Java utilizado pelo Maven:

```bash
./mvnw -version
```

Execute todos os testes a partir de uma compilação limpa:

```bash
./mvnw clean test
```

Para também empacotar e verificar o projeto antes do Pull Request:

```bash
./mvnw clean verify
```

## Windows (PowerShell)

Confira o Java utilizado pelo Maven:

```powershell
.\mvnw.cmd -version
```

Execute todos os testes a partir de uma compilação limpa:

```powershell
.\mvnw.cmd clean test
```

Para também empacotar e verificar o projeto antes do Pull Request:

```powershell
.\mvnw.cmd clean verify
```

## Executar somente os testes de persistência

Linux/macOS:

```bash
./mvnw -Dtest=DomainPersistenceTests test
```

Windows (PowerShell):

```powershell
.\mvnw.cmd "-Dtest=DomainPersistenceTests" test
```

## Conferir o resultado

O resumo deve indicar **zero falhas e zero erros**, seguido de `BUILD SUCCESS`. A quantidade de testes pode aumentar conforme o projeto evoluir.

Os testes usam H2 em memória e não exigem PostgreSQL nem a aplicação iniciada separadamente. Os testes de persistência verificam restrições de propósito; mensagens de violação de constraint podem aparecer mesmo quando o teste passa. Confira sempre o resumo final.

Os relatórios ficam em `target/surefire-reports/`. Em caso de falha, consulte o relatório da classe indicada antes de enviar o PR.

Se aparecer erro de versão do Java, confira `JAVA_HOME` e a saída do comando de versão do Maven. Se o wrapper não tiver permissão de execução no Linux/macOS, execute `chmod +x mvnw`.

## Segurança HTTP (K04)

Veja [autenticação e autorização](k04_authentication_authorization.md) para sessão, logout, configuração de chave e matriz de permissões. A classe `SecurityHttpTests` valida a API em servidor real com H2 isolado, incluindo tokens inválidos e acesso entre condomínios. Seus handlers de prova são exclusivos dos testes; não substituem os endpoints de negócio dos próximos cartões.

`SecurityHttpTests` também verifica o status e o envelope do erro MVC 415, com e sem token,
e a proteção de chamadas diretas a `/error`. `ComplaintFilterHttpTests` cobre o limite de
deslocamento JPA (`2147483647`) e sua rejeição com 400 após ajustar o tamanho da página.
`ComplaintLifecycleHttpTests` exige 200 na listagem de outro condomínio antes de conferir
que `content` não contém o protocolo da denúncia original. Na coleção geral, os passos
87a e 87b permitem conferir manualmente a rejeição de paginação e o erro 415.

```powershell
.\mvnw.cmd "-Dtest=SecurityHttpTests" test
```

## Imagens de denúncias (K14)

`ComplaintImagesHttpTests` verifica por HTTP com H2 o envio multipart, os limites
de upload, a persistência e recuperação da imagem sanitizada e as permissões de
acesso. Execute a classe na raiz do projeto.

```powershell
.\mvnw.cmd "-Dtest=ComplaintImagesHttpTests" test
```

Para executar as chamadas manualmente ou pelo Postman CLI, use o
[roteiro independente do K14](card-collections/k14/README.md).

## Privacidade e proteção contra abuso (K17)

Execute com JDK 25 e H2 isolado:

```bash
./mvnw -Dtest=RequestWindowTests,PrivacyAbuseHttpTests,ComplaintImagesHttpTests,SecurityHttpTests test
./mvnw clean verify
```

No PowerShell, coloque o argumento dos testes entre aspas:

```powershell
.\mvnw.cmd "-Dtest=RequestWindowTests,PrivacyAbuseHttpTests,ComplaintImagesHttpTests,SecurityHttpTests" test
.\mvnw.cmd clean verify
```

O roteiro e a matriz de evidências estão em [K17](k17_privacy_abuse_protection.md). Testes HTTP de outros cards devem isolar seus contadores por cenário para não consumir a cota do cenário seguinte. Não desabilite os limites da aplicação para validar K17.

Para testes manuais ou via Newman, use a [coleção independente da K17](card-collections/k17/README.md). Ela inclui preparação, verificação de privacidade e limites com recuperação após expiração.

## Homologação integrada (K18)

A [matriz de homologação](k18_backend_acceptance.md) relaciona os 13 critérios do MVP às evidências HTTP/H2, informa a base validada e descreve a reprodução com o JAR e as coleções.

Para homologar por um único roteiro HTTP, use a [coleção independente da K18](card-collections/k18/README.md), com preparação e resultados esperados por requisição.
