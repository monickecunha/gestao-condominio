# Gestão de Condomínio

## Como executar

Instale o **JDK 25** e configure `JAVA_HOME`. O Maven Wrapper usa a versão de Maven do projeto e baixa as dependências no primeiro build. A versão homologada do backend está na branch `dev`.

### Linux e macOS

```bash
git clone --branch dev https://github.com/monickecunha/gestao-condominio.git
cd gestao-condominio
bash mvnw --batch-mode --no-transfer-progress clean verify
java -jar target/gestao-condominio-0.0.1-SNAPSHOT.jar --spring.profiles.active=setup
```

### Windows (PowerShell)

```powershell
git clone --branch dev https://github.com/monickecunha/gestao-condominio.git
cd gestao-condominio
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
java -jar target/gestao-condominio-0.0.1-SNAPSHOT.jar --spring.profiles.active=setup
```

O build executa os testes com H2 e gera o JAR. O perfil `setup` inicia a API na porta **8080** com H2 vazio em memória. Os dados são temporários e se perdem ao encerrar o processo com `Ctrl+C`. A demonstração funciona localmente com a API e um cliente HTTP.

## Documentação

Os documentos da aplicação estão na pasta [docs](docs/):

- [Planejamento do MVP](docs/planning.md)
- [Regras de negócio](docs/businessRules.md)
- [Histórias de usuário](docs/userStories.md)
- [Decisões técnicas](docs/k01_technical_decisions_summary.md)
- [Contrato da API](docs/api_contract.md)
- [Arquitetura e entidades](docs/architecture_and_entities.md)
- [Diagrama relacional](docs/condominium_er_diagram.md)
- [Privacidade e proteção contra abuso (K17)](docs/k17_privacy_abuse_protection.md)
- [Homologação do backend (K18)](docs/k18_backend_acceptance.md)
- [Entrega e demonstração do backend (K19)](docs/k19_backend_delivery.md)
- [Comandos de testes](docs/testing.md)
- [Guia do Kanban](docs/kanban.md)
- [Dados dos cartões do Kanban](docs/kanban-items.json)
