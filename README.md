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

## Interface de demonstração

Após iniciar a aplicação, abra [Entre nós](http://localhost:8080/). O front end usa HTML,
CSS e JavaScript puro, em arquivos separados em `src/main/resources/static/`, sem build
adicional. Todas as operações usam a API real na mesma origem.

Para demonstrar do início, use o perfil `setup`: cadastre o condomínio, entre como síndico,
crie uma unidade e anote seu ID. Saia da conta, cadastre um morador com esse ID, entre
novamente como síndico e aprove o vínculo. Entre como morador para enviar uma ocorrência
e guarde o protocolo. O síndico pode consultar detalhes, imagens, atualizar o status e
registrar providências; o acompanhamento por protocolo é público. O responsável aprovado
também pode solicitar vínculos adicionais pela área Minha conta.

O token e o rascunho ficam apenas na memória da aba. Recarregar encerra a sessão local;
não armazene dados reais em uma demonstração. Reiniciar o perfil `setup` apaga os dados H2.
Não abra `index.html` diretamente pelo explorador: os formulários precisam do servidor.

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
