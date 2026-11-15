# K17 — Privacidade e proteção contra abuso

Importe [k17-privacy-abuse.postman_collection.json](k17-privacy-abuse.postman_collection.json). A coleção prepara seus próprios dados, sem depender da coleção geral.

## Preparação e execução

Com JDK 25 e a implementação K17, execute na raiz:

```bash
bash mvnw spring-boot:run -Dspring-boot.run.profiles=setup
```

No PowerShell: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=setup"`.

Comece com H2 vazio, reiniciando a aplicação antes de cada execução completa. O perfil `setup` apaga os dados ao reiniciar. Use as cotas padrão: login 30/minuto, denúncias 10/minuto, cadastros 20/minuto e consulta pública 10/minuto. Ajuste `baseUrl` se necessário.

Execute a coleção inteira no Collection Runner, **uma iteração, sem chamadas paralelas**. Tokens, IDs e protocolo são salvos automaticamente. Também é possível enviar os passos em ordem manualmente. Não execute outras coleções durante o roteiro.

Os passos 13 e 18 aguardam **61 segundos automaticamente** antes da requisição; aguarde terminar. O roteiro leva aproximadamente 2–3 minutos. Configure o timeout de scripts para mais de 61 segundos, se seu executor impuser um limite. Os passos 13–16 fazem chamadas adicionais sequenciais via `pm.sendRequest`: a resposta principal mostra 401 ou 404/400, e o **429 aparece nas chamadas do script e em Test Results**. Confira todas as asserções, não apenas o status da resposta principal. Cada sequência de tentativas deve terminar em menos de um minuto.

Para repetir somente os limites, execute 13–18 em ordem; o passo 13 expira as cotas da preparação. Não repita 01–07 sem reiniciar o servidor.

## Requisições e resultados esperados

| Nome no Postman | HTTP principal | Resultado esperado |
|---|---|---|
| 01. Cadastrar condomínio e síndico | 201 | Condomínio e síndico cadastrados. |
| 02. Fazer login como síndico | 200 | Login do síndico; salva managerToken e condoId. |
| 03. Criar unidade 101 | 201 | Unidade 101 criada; salva unitId. |
| 04. Solicitar vínculo de morador | 201 | Vínculo de Ana pendente; salva anaBindingId. |
| 05. Aprovar Ana | 204 | Vínculo aprovado; resposta sem corpo. |
| 06. Fazer login como morador aprovado | 200 | Login do morador aprovado; salva residentToken. |
| 07. Registrar texto malicioso como dado privado | 201 | Denúncia criada; recibo contém somente message e protocol, com UUID v4. Salva complaintProtocol. |
| 08. Conferir lista administrativa e salvar ID | 200 | Uma denúncia; salva managerComplaintId. |
| 09. Conferir detalhe privado sem identificação do autor | 200 | Texto enviado preservado em fullDescription; CPF do morador ausente. |
| 10. Conferir campos públicos do protocolo | 200 | Seis campos públicos; sem texto privado, anexos ou dados do autor. |
| 11. Negar detalhe administrativo ao morador | 403 | 403, sem detalhe da denúncia. |
| 12. Recusar tipo inválido no login sem ecoar dados | 400 | Invalid request body; sem CPF, senha ou valor inválido na resposta. |
| 13. Esgotar login e ignorar IP forjado | 401 | Aguarda 61 s; 30 tentativas retornam 401 e a 31ª retorna 429 com cabeçalhos. Os scripts variam Forwarded e X-Forwarded-For. |
| 14. Esgotar denúncias e bloquear envio autenticado | 401 | 10 tentativas sem token retornam 401; a 11ª, com token e corpo válidos, retorna 429. |
| 15. Compartilhar cota de cadastros | 400 | 20 corpos inválidos, alternando cadastros, retornam 400; vínculo adicional na 21ª tentativa retorna 429. |
| 16. Limitar consultas de protocolos inexistentes | 404 | 10 consultas retornam 404; a 11ª retorna 429. |
| 17. Confirmar que envio bloqueado não criou denúncia | 200 | totalElements permanece 1; listagem não compartilha a cota de criação. |
| 18. Recuperar consulta após expiração | 404 | Aguarda 61 s; volta a retornar 404, em vez de 429. |

## Escopo e evidências complementares

Esta coleção confere respostas HTTP, limites por origem, tentativa de contornar cotas por cabeçalhos, ausência de gravação após bloqueio e expiração. Usa somente dados fictícios. Não comprova sozinha ausência de dados no H2/logs, concorrência, limite de memória, sanitização dos bytes de imagens ou isolamento entre dois condomínios. Esses cenários são cobertos pelas classes Java descritas na [matriz da K17](../../k17_privacy_abuse_protection.md). A [coleção K14](../k14/README.md) complementa os testes de anexos.

```bash
bash mvnw -Dtest=RequestWindowTests,PrivacyAbuseHttpTests,ComplaintImagesHttpTests,SecurityHttpTests test
```

Com Newman instalado, use timeout compatível com as esperas:

```bash
newman run docs/card-collections/k17/k17-privacy-abuse.postman_collection.json --timeout-script 90000
```

## Validação realizada

Em 15/11/2026, execução completa com Newman, JDK 25, perfil `setup`, H2 vazio e cotas padrão: **88 requisições, 94 asserções, nenhuma falha**, em 2 minutos e 17 segundos. Inclui as chamadas adicionais feitas pelos scripts.
