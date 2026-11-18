# K19 — Entrega e demonstração do backend

A entrega consiste no JAR executável da API, na suíte de testes com H2 e nos roteiros HTTP. Use um checkout limpo da revisão de entrega e o perfil `setup`, que inicia o H2 em memória sem cadastros. A aplicação e os testes funcionam localmente sem serviços de banco externos ou frontend.

## Preparar a execução

1. Instale o JDK 25, configure `JAVA_HOME` e confirme com `java -version` e `bash mvnw -version` ou `.\mvnw.cmd -version`.
2. Siga os [comandos de build e execução](../README.md#como-executar). `clean verify` executa a suíte com H2 e gera `target/gestao-condominio-0.0.1-SNAPSHOT.jar`. O Maven Wrapper baixa Maven e dependências no primeiro uso.
3. Depois da mensagem de inicialização, use `http://localhost:8080/api/v1` como `baseUrl`. Se a porta estiver ocupada, passe `--server.port=8081` ao JAR e ajuste o cliente HTTP.
4. Importe a [coleção K18](card-collections/k18/k18-backend-acceptance.postman_collection.json). Ela cria os próprios dados fictícios e armazena os tokens, IDs e protocolos usados nas próximas chamadas.

Veja o [contrato HTTP](api_contract.md) para corpos, papéis e códigos de retorno e o [guia de testes](testing.md) para executar cenários específicos.

## Apresentar o fluxo principal

Em uma instância recém-iniciada, execute manualmente as requisições abaixo na ordem indicada. Use os números da coleção K18. Os passos de listagem dependem das duas denúncias criadas na etapa anterior.

| Passos | O que mostrar | Retornos |
|---|---|---|
| 01–08 | Cadastro do condomínio e do síndico, criação da unidade 101, pedido de vínculo e aprovação de Ana. Mostre a negativa quando ela tenta denunciar enquanto pendente. | `201` nos cadastros, `200` nos logins, `403` nas negativas e `204` na aprovação. |
| 15, 16, 21 e 23 | Ana envia duas denúncias e recebe protocolos distintos. O síndico busca a denúncia com local na listagem e abre o detalhe. | `201` no envio e `200` nas consultas administrativas. |
| 28, 31–33 e 35 | O síndico abre análise, registra a vistoria, muda o status para `IN_PROGRESS` e resolve. O detalhe administrativo mostra a providência. | `201` na providência e `200` nas transições e no detalhe. |
| 36 | Consulte o mesmo protocolo sem token. Mostre as três transições e o status `RESOLVED`. | `200` com o histórico público. A providência fica no detalhe administrativo. |

Para apresentar anexos, encerre e reinicie o JAR e execute o [roteiro K14](card-collections/k14/README.md) com a imagem fictícia fornecida. Mostre o download pelo síndico e a negativa com o token do morador.

## Ensaiar a entrega completa

Reinicie o JAR antes de executar a [coleção completa K18](card-collections/k18/README.md). Use uma iteração e o diretório de trabalho `docs/card-collections/k18`, onde está a imagem de exemplo. Essa coleção percorre 65 passos incluindo rejeição, filtros, imagens, morador adicional e limite de acesso. O primeiro passo de imagens tem uma espera automática de 61 segundos para liberar a cota de denúncias.

Com Postman CLI instalado, abra um terminal na pasta da coleção e execute:

```bash
postman collection run k18-backend-acceptance.postman_collection.json --timeout-script 90000 --no-insecure-file-read --no-report-events
```

Confira a contagem de falhas ao final. Respostas `400`, `401`, `403`, `409` e `429` são esperadas nos cenários negativos da coleção. O [roteiro K18](card-collections/k18/README.md) também traz o comando com Newman. A [matriz K18](k18_backend_acceptance.md) associa os cenários aos critérios do MVP e aos testes automatizados.

## Limitações da demonstração

- O H2 em memória perde os dados ao encerrar a API. Reinicie antes de repetir um roteiro que cadastra o condomínio. O perfil `test`, selecionado por padrão ao omitir `setup`, carrega dados de exemplo e pode causar conflitos de cadastro com a coleção.
- A aplicação admite um condomínio por instalação. Os testes automatizados exercitam o isolamento entre condomínios com um H2 separado, preparado para esse cenário.
- O JWT vale duas horas. Sem `JWT_SECRET`, a aplicação gera uma chave por inicialização e os tokens da sessão anterior deixam de funcionar. As cotas de acesso são locais ao processo.
- O cadastro adicional cria um vínculo pendente para aprovação pelo síndico. A coleção usa dados e imagem fictícios.

## Evidências de entrega

O [K18](k18_backend_acceptance.md) registra a homologação técnica de **15/11/2026**. A verificação de **16/11/2026** usou uma cópia limpa da `dev` em `a9eac9185ba971230060c2f05eff672e5efe0a64`, com o K18 integrado. O JAR foi iniciado em duas instâncias independentes, ambas com o perfil `setup`, endereço local e H2 em memória.

| Verificação | Resultado |
|---|---|
| Maven Wrapper `clean verify` no Windows com JDK 25 | `BUILD SUCCESS`, 81 testes, zero falhas, erros ou ignorados. JAR empacotado. |
| Postman CLI 1.90.0, roteiro curto | 18 requisições e 28 asserções, zero falhas. |
| Postman CLI 1.90.0, coleção completa K18 | 75 requisições e 107 asserções, zero falhas. O total inclui as consultas auxiliares dos scripts. |

A revisão por outro integrante e a integração do K19 via PR seguem pendentes.
