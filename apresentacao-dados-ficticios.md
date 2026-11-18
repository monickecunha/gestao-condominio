Para uma apresentação rápida, use o perfil **`test`**, que já carrega os dados fictícios. O perfil `setup` indicado anteriormente começa vazio.

```java
java -jar target/gestao-condominio-0.0.1-SNAPSHOT.jar --spring.profiles.active=test
```

Abra [Entre nós](http://localhost:8080/).

Os dados estão em [TestConfig.java](src/main/java/residencia/gestao_condominio/config/TestConfig.java:41):

| Perfil | Nome | CPF de acesso | Senha | Situação |
|---|---|---|---|---|
| Síndico | Carlos Silva | `11122233344` | `123456` | Administra o condomínio |
| Moradora | Ana Souza | `22233344455` | `123456` | Aprovada, unidade 101 — Bloco A |
| Morador | Lucas Oliveira | `33344455566` | `123456` | Pendente, unidade 202 — Bloco B |

O condomínio é o **Residencial Bella Vista**. Já existe uma denúncia de barulho com status **Recebida**. Deixe o campo **ID do condomínio** vazio no login.

**Roteiro de apresentação — aproximadamente 10 minutos**

1. **Apresente a página inicial.** Explique que o morador se autentica para enviar, mas sua identidade não fica vinculada à denúncia.

2. **Entre como Ana**, em **Minha conta**. Registre uma ocorrência com:

   | Campo | Conteúdo |
   |---|---|
   | Assunto | Uso das áreas comuns |
   | Descrição | Há caixas obstruindo a passagem no salão de convivência desde esta manhã. Solicito a liberação do acesso. |
   | Local | Salão de convivência |

   Revise, envie e **copie o protocolo**.

3. **Saia da conta e consulte o protocolo.** Mostre que o acompanhamento funciona sem login e apresenta o status **Recebida**.

4. **Entre como Carlos** e abra **Painel do síndico → Ocorrências → Ver detalhes**. Mostre a descrição e o local. Altere para **Em análise** e registre uma providência:

   > Equipe de apoio acionada para verificar a passagem e organizar a retirada das caixas.

5. **Avance o atendimento** para **Em andamento** e depois **Resolvida**. Consulte novamente o protocolo para mostrar o histórico público. A providência interna não aparece nessa consulta.

6. **Demonstre a aprovação de moradores.** Entre como Lucas e tente enviar uma ocorrência: o vínculo pendente impede o envio. Depois, entre como Carlos, acesse **Vínculos pendentes** e aprove Lucas. Entre novamente como Lucas para demonstrar o envio autorizado.

Ao reiniciar a aplicação, o H2 em memória perde as alterações e o perfil `test` recria os dados iniciais. O protocolo da ocorrência também muda; copie sempre o gerado naquela apresentação.
