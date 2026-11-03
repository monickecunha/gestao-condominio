# Gestão de Condomínio

## Como executar

Instale o **JDK 25** e configure `JAVA_HOME`. Não é necessário instalar Maven: o projeto inclui o Maven Wrapper.

### Linux e macOS

```bash
git clone https://github.com/monickecunha/gestao-condominio.git
cd gestao-condominio
chmod +x mvnw
./mvnw spring-boot:run
```

### Windows (PowerShell)

```powershell
git clone https://github.com/monickecunha/gestao-condominio.git
cd gestao-condominio
.\mvnw.cmd spring-boot:run
```

Se já possui o repositório, execute apenas o comando de inicialização dentro da pasta do projeto.

A aplicação utiliza a porta **8080**. Para encerrar, pressione `Ctrl+C`.

## Documentação

Os documentos da aplicação estão na pasta [docs](docs/):

- [Planejamento do MVP](docs/planning.md)
- [Regras de negócio](docs/businessRules.md)
- [Histórias de usuário](docs/userStories.md)
- [Decisões técnicas](docs/k01_technical_decisions_summary.md)
- [Contrato da API](docs/api_contract.md)
- [Arquitetura e entidades](docs/architecture_and_entities.md)
- [Diagrama relacional](docs/diagrama_er_condominio.md)
- [Guia do Kanban](docs/kanban.md)
- [Dados dos cartões do Kanban](docs/kanban-items.json)
