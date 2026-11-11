# Regras de negócio

| Código | Regra de negócio |
|---|---|
| RN01 | Somente moradores cadastrados, autenticados e autorizados poderão registrar denúncias. |
| RN02 | Toda denúncia deverá estar associada a um condomínio. |
| RN03 | A denúncia não deverá possuir vínculo direto com a conta do denunciante. |
| RN04 | A descrição e a categoria da denúncia serão obrigatórias. |
| RN05 | O envio de imagens será opcional. |
| RN06 | Cada denúncia deverá possuir um protocolo único de acompanhamento. |
| RN07 | Toda denúncia deverá iniciar com o status `RECEIVED`. |
| RN08 | Somente administradores autorizados poderão atualizar o status das denúncias. |
| RN09 | Toda alteração de status deverá ser registrada no histórico. |
| RN10 | A rejeição de uma denúncia deverá possuir justificativa. |
| RN11 | Somente administradores autorizados poderão aprovar ou rejeitar vínculos de moradores com unidades. |
| RN12 | O sistema deverá minimizar a coleta e o armazenamento de dados que possam identificar o denunciante. |
| RN13 | Um vínculo rejeitado não permite login no respectivo condomínio. Se todos os vínculos do morador nesse condomínio estiverem rejeitados, o sistema deverá recusar o login. |

---