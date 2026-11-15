# K14 — Envio e recuperação de imagens

O roteiro usa um servidor local com H2 vazio e uma [imagem PNG fictícia](imagem-exemplo.png).
Inicie a aplicação com o perfil `setup`. Reinicie o servidor para repetir a coleção desde o
primeiro passo.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=setup"
```

Em outro terminal, partindo da raiz do projeto, entre na pasta do roteiro e rode
a [coleção do K14](k14-complaint-images.postman_collection.json) com o Postman CLI.

```powershell
Set-Location docs/card-collections/k14
postman collection run k14-complaint-images.postman_collection.json --no-insecure-file-read --no-report-events --bail failure
```

Para executar no Postman Desktop, importe a coleção e selecione
`docs/card-collections/k14` como diretório de trabalho. Nas requisições 07 a 09,
confira se os campos de arquivo `images` apontam para `imagem-exemplo.png`.
Preserve o tipo `image/jpeg` explícito na requisição 08 para reproduzir a divergência.
Se a aplicação estiver em outra porta, ajuste a variável `baseUrl`.

| Passos | Resultado esperado |
|---|---|
| 01–06 | Preparam condomínio, síndico, unidade e morador aprovado. Os tokens são obtidos no login. |
| 07 | Envia a denúncia com PNG e recebe `201` com protocolo e mensagem. |
| 08–09 | Recebem `400` por MIME incompatível e por quatro imagens. |
| 10–12 | A API lista apenas a denúncia válida. O detalhe traz a URL do anexo e o download retorna PNG sem o texto fictício presente nos metadados originais. |
| 13–14 | O morador recebe `403` ao acessar o anexo. Sem token, a API retorna `401`. |
| 15 | A consulta pública retorna apenas os campos de acompanhamento. |

O roteiro foi validado com Postman CLI 1.90.0 em H2 vazio, com 15 requisições e
20 asserções aprovadas.

Os testes HTTP automatizados `ComplaintImagesHttpTests` cobrem também os limites de
tamanho, os bytes recuperados e o isolamento com dois condomínios em H2.
O perfil `setup` da coleção segue a restrição padrão de um condomínio por instalação.
