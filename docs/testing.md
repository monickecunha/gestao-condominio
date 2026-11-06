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

```powershell
.\mvnw.cmd "-Dtest=SecurityHttpTests" test
```
