# K18 — Homologação do backend

Importe [k18-backend-acceptance.postman_collection.json](k18-backend-acceptance.postman_collection.json). O roteiro é independente: prepara seus próprios dados e cobre cadastro, pendência/rejeição, denúncias, transições, providências, filtros, imagens, cadastro adicional e limite de consulta.

## Preparar

Com JDK 25, na raiz do projeto:

```bash
bash mvnw clean verify
java -jar target/gestao-condominio-0.0.1-SNAPSHOT.jar --spring.profiles.active=setup
```

No PowerShell, substitua `bash mvnw` por `.\mvnw.cmd`.

Comece com H2 vazio e cotas padrão. Encerre e reinicie a API antes de repetir a coleção. Ajuste `baseUrl` se a API não estiver em `http://localhost:8080/api/v1`. Não execute outras coleções ao mesmo tempo no mesmo servidor.

No Postman, configure o diretório de trabalho para **docs/card-collections/k18**. Nos três envios de imagens, confira se os campos `images` apontam para [sample-image.png](sample-image.png). No cenário de MIME incompatível, preserve `image/jpeg` mesmo usando um PNG.

## Executar

Execute todas as pastas em ordem no Collection Runner, **uma iteração**, ou envie as requisições manualmente em ordem. Tokens, IDs, protocolos e datas são preenchidos automaticamente.

A primeira requisição da pasta **5. Imagens e privacidade** aguarda **61 segundos automaticamente** para expirar a cota de denúncias usada antes. Permita scripts com timeout superior a 61 segundos. Durante essa espera, o Postman mostra Running Scripts. A coleção costuma levar 1–3 minutos.

O último passo envia dez consultas auxiliares antes da requisição principal: a resposta principal deve ser **429**. Aguarde um minuto antes de repetir consultas públicas depois dele. Os erros 400/401/403/409 são resultados esperados nas negativas; confira Test Results, não apenas o código HTTP.

Para Newman, entre no diretório da coleção antes de executar:

```bash
cd docs/card-collections/k18
newman run k18-backend-acceptance.postman_collection.json --timeout-script 90000
```

## Requisições e resultados esperados

| Nome no Postman | HTTP | Resultado esperado |
|---|---|---|
| 01. Cadastrar condomínio e síndico | 201 | Condomínio e síndico criados. |
| 02. Fazer login como síndico | 200 | Login do síndico; salva managerToken e condoId. |
| 03. Criar unidade 101 | 201 | Unidade 101 criada; salva unitId. |
| 04. Solicitar vínculo de morador | 201 | Vínculo de Ana criado como PENDING; salva anaBindingId. |
| 05. Fazer login como morador pendente | 200 | Morador pendente autentica; salva residentToken. |
| 06. Tentar autoaprovação de Ana | 403 | Morador não pode aprovar o próprio vínculo. |
| 07. Impedir denúncia de morador pendente | 403 | 403; vínculo pendente permite login, mas não autoriza denúncia. |
| 08. Aprovar Ana | 204 | Síndico aprova Ana; o token já emitido passa a permitir denúncias. |
| 09. Criar unidade 102 para o teste de rejeição | 201 | Unidade 102 criada para o cenário de rejeição. |
| 10. Solicitar vínculo de Ana na unidade 102 | 201 | Outra conta solicita vínculo na unidade 102; salva firstRequestId. |
| 11. Entrar como Ana da unidade 102 | 200 | Conta pendente da unidade 102 autentica; salva rejectedResidentToken. |
| 12. Síndico rejeita Ana da unidade 102 | 204 | Pedido da unidade 102 rejeitado; resposta sem corpo. |
| 13. Fazer login após a rejeição de Ana | 403 | Conta com vínculo rejeitado não consegue novo login. |
| 14. Tentar denunciar com vínculo rejeitado | 403 | Token emitido antes da rejeição não autoriza denúncia. |
| 15. Registrar denúncia sem local | 201 | Recibo com protocolo UUID v4 |
| 16. Registrar outra denúncia e comparar protocolos | 201 | Protocolos diferentes |
| 17. Enviar descrição em branco | 400 | Entrada inválida recusada. |
| 18. Enviar categoria inválida | 400 | Entrada inválida recusada. |
| 19. Tentar registrar sem autenticação | 401 | Requisição não autenticada recusada. |
| 20. Tentar registrar como síndico | 403 | Acesso sem permissão recusado. |
| 21. Conferir primeira página de denúncias | 200 | Página 0 com uma denúncia: segunda cadastrada; totalElements 2, totalPages 2. |
| 22. Conferir segunda página de denúncias | 200 | Página 1 com a primeira denúncia; totalElements 2, totalPages 2. |
| 23. Consultar detalhes da denúncia com local | 200 | Detalhe da segunda denúncia: NOISE, RECEIVED, descrição e local enviados. |
| 24. Tentar listar denúncias como morador | 403 | Acesso sem permissão recusado. |
| 25. Tentar listar denúncias sem autenticação | 401 | Requisição não autenticada recusada. |
| 26. Impedir resolução direta | 409 | RECEIVED → RESOLVED recusado com 409. |
| 27. Recusar atualização pelo morador | 403 | Morador não pode alterar status. |
| 28. Abrir análise da denúncia com local | 200 | RECEIVED → UNDER_REVIEW registrado. |
| 29. Exigir login para registrar providência | 401 | Providência exige autenticação. |
| 30. Recusar providência em branco | 400 | Providência em branco recusada. |
| 31. Registrar vistoria como síndico | 201 | Vistoria no corredor registrada por Maria, com data válida. |
| 32. Iniciar providências | 200 | UNDER_REVIEW → IN_PROGRESS registrado. |
| 33. Resolver a denúncia | 200 | IN_PROGRESS → RESOLVED registrado. |
| 34. Impedir reabertura após resolução | 409 | Denúncia resolvida não pode voltar para IN_PROGRESS. |
| 35. Conferir providência nos detalhes administrativos | 200 | Detalhe administrativo contém a providência registrada. |
| 36. Acompanhar resolução pelo protocolo | 200 | RESOLVED com três transições no histórico público; sem providência interna. |
| 37. Abrir análise da denúncia sem local | 200 | Primeira denúncia passa de RECEIVED para UNDER_REVIEW. |
| 38. Exigir justificativa para rejeitar | 400 | Rejeição sem justificativa recusada. |
| 39. Rejeitar com motivo público | 200 | UNDER_REVIEW → REJECTED com justificativa pública. |
| 40. Acompanhar rejeição pelo protocolo | 200 | REJECTED com duas transições e justificativa Ocorrência não confirmada. |
| 41. registrar denúncia de segurança | 201 | Terceira denúncia criada: SECURITY / RECEIVED. |
| 42. conferir ausência de filtros e preparar datas | 200 | Três denúncias na ordem esperada; prepara datas no fuso de Brasília. |
| 43. filtrar somente por categoria | 200 | Filtro SECURITY retorna somente a terceira denúncia. |
| 44. filtrar somente por status | 200 | Filtro REJECTED retorna somente a primeira denúncia. |
| 45. combinar os quatro filtros | 200 | Categoria, status e período combinados retornam somente a denúncia resolvida. |
| 46. recusar categoria inválida | 400 | Categoria inválida recusada com erro de validação. |
| 47. recusar período invertido | 400 | Período invertido recusado com erro de validação. |
| 48. Enviar imagem com uma denúncia | 201 | Aguarda 61 segundos; cria a quarta denúncia, com PNG. Recibo contém somente message e protocol. |
| 49. Recusar imagem com MIME incompatível | 400 | PNG declarado como image/jpeg é recusado. |
| 50. Recusar quatro imagens | 400 | Quatro imagens são recusadas; limite é três. |
| 51. Localizar denúncia como síndico | 200 | Exatamente quatro denúncias; a mais recente é a imagem válida. Salva complaintId. |
| 52. Obter URL do anexo nos detalhes administrativos | 200 | Detalhe contém uma referência de anexo; salva imageId. |
| 53. Recuperar PNG como síndico | 200 | Retorna bytes PNG sanitizados, sem o metadado fictício original. |
| 54. Recusar acesso ao anexo como morador | 403 | Morador não pode baixar o anexo administrativo. |
| 55. Exigir autenticação para recuperar anexo | 401 | Download sem token é recusado. |
| 56. Acompanhar denúncia publicamente | 200 | Consulta pública contém somente os seis campos de acompanhamento. |
| 57. Responsável cadastra morador adicional | 201 | Vínculo PENDING na unidade 101; salva additionalBindingId. |
| 58. Autenticar morador adicional pendente | 200 | Login 200; salva additionalToken. |
| 59. Negar denúncia do adicional pendente | 403 | 403; aprovação do responsável não substitui a do síndico. |
| 60. Negar convite para outra unidade | 403 | 403; Ana não é responsável pela unidade 102. |
| 61. Recusar cadastro adicional duplicado | 409 | 409; conta e vínculo originais preservados. |
| 62. Síndico aprova o morador adicional | 204 | Aprovação concluída, sem corpo. |
| 63. Adicional aprovado consegue denunciar | 201 | 201; quinta denúncia criada; recibo sem identificação do morador. |
| 64. Conferir total após todos os fluxos | 200 | totalElements: 5; tentativas inválidas não criaram denúncias extras. |
| 65. Verificar o limite de consultas públicas | 429 | Resposta principal 429 após dez consultas auxiliares; sem dados da denúncia, com Retry-After: 60 e Cache-Control: no-store. |

## Relação com os 13 critérios

| Critérios de planning.md §7 | Evidência na coleção |
|---|---|
| 1 — Cadastro e login do morador | Pastas 1 e 6 |
| 2 — Denúncia por morador aprovado | Pastas 2 e 6; pendentes e rejeitados recebem 403 |
| 3 — Dados obrigatórios | Pasta 2: descrição em branco e categoria inválida |
| 4 — Categoria e imagem opcional | Denúncias JSON na pasta 2 e multipart na pasta 5 |
| 5 — Protocolo | Recibo validado no cadastro, protocolos distintos |
| 6 — Consulta por protocolo | Acompanhamento sem token nas pastas 3 e 5 |
| 7 — Login e acesso administrativo | Login na pasta 1; listagem, detalhe e negativas nas pastas 2 e 3 |
| 8 — Lista do condomínio | Listagem e filtros nas pastas 2 e 4; isolamento entre condomínios exige testes Java |
| 9 — Detalhe e imagens | Pastas 2 e 5, incluindo download e negativas de acesso |
| 10 — Status e providências | Pasta 3 |
| 11 — Histórico | Pasta 3: estados anteriores/seguintes e datas |
| 12 — Andamento atualizado | Pasta 3: consulta pública após resolução e rejeição |
| 13 — Privacidade | Recibos, separação público/interno e remoção do metadado da imagem de exemplo |

Ao final existem cinco denúncias válidas: uma NOISE rejeitada, uma NOISE resolvida, uma SECURITY recebida, uma NOISE recebida com imagem e uma OTHER recebida do morador adicional. As negativas não devem aumentar esse total.

## Limites da evidência

A coleção não inspeciona diretamente H2/logs, não simula concorrência nem cria outro condomínio na mesma instalação. Execute também `bash mvnw clean verify` para comprovar esses cenários e as demais negativas de imagens. A matriz completa e as pendências de revisão cruzada/congelamento estão em [K18](../../k18_backend_acceptance.md). O cadastro adicional representa o convite do MVP; não envia e-mail.

## Validação realizada

Em 15/11/2026, execução com Newman contra o JAR da base `89ae045`, JDK 25, perfil `setup`, H2 vazio e cotas padrão: **75 requisições e 107 asserções, nenhuma falha**, em 1 minuto e 8 segundos. As 75 requisições incluem as dez chamadas auxiliares do último passo.
