# Escopo da entrega — revisão de 02/11/2026

A entrega de **16/11/2026** será focada exclusivamente no **backend/API**. A equipe tem quatro desenvolvedores backend. As funcionalidades e regras abaixo serão implementadas e demonstradas por requisições HTTP e testes automatizados, sem exigir telas.

- **H2 para testes**, conforme decisão da equipe. Para demonstração local, o plano propõe um perfil H2 em memória com dados fictícios e carga reproduzível; essa configuração ainda será implementada em K02 e validada pela equipe.
- Modelagem, repositórios e testes de persistência continuam no backend. PostgreSQL, provisionamento de banco externo, migrações para esse banco e frontend ficam opcionais, somente após o backend estar entregue e se houver tempo.
- A validação será por documentação da API, coleção de requisições HTTP e testes de integração. Ferramentas específicas e mecanismo de autenticação serão definidos em K01/K11.
- A alteração de escopo não significa que o H2 ou os endpoints já estejam implementados.

# 1. Análise do problema — 5 Porquês

**Problema:** Dificuldade dos moradores em realizar denúncias no condomínio sem comprometer sua identidade.

## 1.1. Por quê?

Porque não existe um canal que permita realizar denúncias de forma anônima.

## 1.2. Por quê?

Porque os canais de comunicação disponíveis são o WhatsApp ou o contato direto com a administração, que podem identificar o denunciante.

## 1.3. Por quê?

Porque os meios de comunicação utilizados pelo condomínio não foram estruturados para garantir o anonimato dos moradores.

## 1.4. Por quê?

Porque a administração não implementou um processo específico para o recebimento e o tratamento de denúncias anônimas.

## 1.5. Por quê?

Porque a necessidade de oferecer um canal seguro e anônimo para denúncias não foi identificada ou priorizada pela administração.

### 1.6. Possível causa-raiz

Ausência de identificação ou priorização da necessidade de um canal estruturado e seguro para o registro de denúncias anônimas.

---

# 2. Definição do MVP

## 2.1. Visão geral

O projeto consiste no desenvolvimento de um backend/API de gestão condominial, cuja primeira versão será focada no registro, acompanhamento e gerenciamento de denúncias anônimas.

A solução permitirá que moradores cadastrados e autenticados comuniquem ocorrências relacionadas ao condomínio sem que sua identidade seja diretamente associada às denúncias registradas.

O cadastro e a autenticação serão utilizados para controlar o acesso à plataforma, garantindo que somente moradores autorizados possam registrar denúncias.

Os síndicos e administradores poderão visualizar as denúncias recebidas, analisar as ocorrências, registrar providências e atualizar seus respectivos status.

A aplicação será desenvolvida de forma a permitir sua expansão futura para outras funcionalidades relacionadas à gestão condominial.

## 2.2. Objetivo do MVP

Desenvolver um backend/API funcional que permita o cadastro e a autenticação de moradores, o registro de denúncias anônimas, o acompanhamento das ocorrências por meio de protocolo e o gerenciamento das denúncias pela administração do condomínio.

O MVP tem como objetivo validar a viabilidade da solução, oferecendo um canal estruturado de comunicação entre moradores e administração, com foco na privacidade, organização e acompanhamento das ocorrências.

## 2.3. Público-alvo

O sistema terá dois perfis principais de usuários: Resident e Manager.

### 2.3.1. Moradores (Resident)

Pessoas vinculadas a uma unidade condominial que desejam comunicar problemas ou situações que necessitem da atenção da administração.

Os moradores deverão possuir cadastro e autenticação para acessar as funcionalidades de registro de denúncias.

As denúncias não deverão possuir associação direta com a identidade do morador responsável pelo registro.

### 2.3.2. Síndicos e administradores condominiais (Manager)

Pessoas responsáveis pela administração de um condomínio.

Terão acesso às denúncias registradas no condomínio sob sua responsabilidade, podendo consultar ocorrências, registrar providências e atualizar seus status.

---

# 3. Funcionalidades do MVP

As funcionalidades serão organizadas em módulos, de acordo com as responsabilidades dos usuários e os principais processos do sistema.

## 3.1. Módulo de cadastro e autenticação

**F01 — Cadastro do condomínio**

Disponibilizar uma operação da API para cadastrar o condomínio e o síndico na plataforma, mediante o fornecimento dos dados necessários para identificação e autenticação.

**F02 — Cadastro dos usuários**

Permitir o cadastro dos usuários na plataforma, mediante o fornecimento dos dados necessários para identificação e autenticação e enviar para aprovação do síndico.

**F03 — Autenticação de usuários**

Permitir que moradores, síndicos acessem o sistema utilizando suas credenciais.

O acesso às funcionalidades deverá respeitar as permissões de cada perfil.

**F04 — Cadastro de moradores vinculados à unidade**

Permitir que o morador responsável por uma unidade cadastre ou convide outros moradores vinculados à mesma unidade, conforme as regras estabelecidas.

**F05 — Gerenciamento de contas**

Aprovar o cadastro dos usuários.

## 3.2. Módulo de denúncias

**F06 — Registro de denúncias anônimas**

Permitir que moradores autenticados registrem denúncias por meio de requisições à API.

O sistema não deverá associar diretamente a identidade do morador à denúncia registrada.

**F07 — Categorização de denúncias**

Permitir que o morador selecione uma categoria predefinida para classificar a ocorrência.

**Categorias previstas:**

- Barulho excessivo;
- Problemas de segurança;
- Uso inadequado de áreas comuns;
- Descarte irregular de lixo;
- Descumprimento das regras do condomínio;
- Outros.

**F08 — Descrição da ocorrência**

Permitir que o morador descreva a situação que deseja comunicar à administração.

A descrição será obrigatória.

**F09 — Identificação do local da ocorrência**

Permitir que o morador informe o local onde ocorreu o problema, sem exigir informações que comprometam desnecessariamente seu anonimato. (campo opcional)

**F10 — Inclusão de imagens**

Permitir que o morador anexe imagens à denúncia para auxiliar na análise da ocorrência.

O envio de imagens será opcional.

Os arquivos deverão passar por validações de formato, tamanho e segurança antes do armazenamento.

**F11 — Validação dos dados da requisição**

Validar as informações obrigatórias antes de permitir o envio da denúncia.

Caso existam informações inválidas ou ausentes, a API deverá retornar erros de validação com mensagens orientando a correção.

## 3.3. Módulo de acompanhamento

**F12 — Geração de protocolo**

Gerar automaticamente um protocolo único de acesso após o registro da denúncia.

Essa informação vai permitir que o morador acompanhe a ocorrência posteriormente.

**F13 — Consulta de denúncias**

Permitir que o morador consulte o andamento de uma denúncia utilizando o protocolo recebido.

A consulta deverá apresentar informações limitadas ao acompanhamento da ocorrência, sem expor dados internos da administração.

**F14 — Histórico de andamento**

Permitir que o morador acompanhe as alterações de status da denúncia.

Informações administrativas internas e dados que possam comprometer a privacidade de terceiros não deverão ser disponibilizados.

## 3.4. Módulo de gestão administrativa

**F15 — Visualização das denúncias recebidas**

Permitir que síndicos e administradores visualizem as denúncias registradas no condomínio sob sua responsabilidade.

A listagem deverá apresentar informações como protocolo, categoria, data de registro e status.

**F16 — Consulta dos detalhes da denúncia**

Permitir que administradores autorizados consultem as informações completas das ocorrências, incluindo descrição, categoria, local informado e imagens anexadas.

**F17 — Atualização do status**

Permitir que administradores alterem o status das denúncias conforme o andamento da análise e das providências adotadas.

As alterações deverão respeitar os status e as regras de transição definidos pelo sistema.

**F18 — Registro de providências**

Permitir que administradores registrem as ações realizadas em relação a uma denúncia.

Cada registro deverá armazenar a descrição da providência e a data.

**F19 — Histórico de alterações de status**

Registrar as alterações de status realizadas pela administração.

Cada alteração deverá conter:

- Novo status;
- Data e horário da alteração;
- Justificativa, quando aplicável.

**F20 — Filtros de denúncias**

Permitir que administradores filtrem as denúncias por categoria, status e período de registro.

**F21 — Gerenciamento de moradores e unidades**

Permitir que administradores autorizados:

- Aprovem ou rejeitem solicitações de vínculo com unidades;

---

# 4. Status das denúncias

As denúncias poderão assumir os seguintes status:

| Status | Identificador | Descrição |
|---|---|---|
| Recebida | `RECEIVED` | Denúncia registrada no sistema, aguardando análise da administração. |
| Em análise | `UNDER_REVIEW` | Denúncia sendo avaliada pela administração para verificar a ocorrência e definir possíveis providências. |
| Em andamento | `IN_PROGRESS` | Denúncia já analisada, com providências sendo executadas para solucionar a ocorrência. |
| Resolvida | `RESOLVED` | Ocorrência atendida e considerada finalizada pela administração. |
| Rejeitada | `REJECTED` | Denúncia analisada e não acolhida pela administração, mediante justificativa. |

## 4.1. Regras de atualização

- Toda denúncia deverá iniciar com o status `RECEIVED`.
- Somente administradores condominiais autorizados poderão alterar o status.
- A alteração deverá ser registrada no histórico da denúncia.
- Denúncias poderão ser rejeitadas após análise, mediante justificativa.
- A atualização deverá ser refletida na consulta realizada pelo morador.

## 4.2. Fluxo de status

O fluxo principal será:

`RECEIVED → UNDER_REVIEW → IN_PROGRESS → RESOLVED`

Quando uma denúncia não for acolhida pela administração, poderá seguir o fluxo:

`RECEIVED → UNDER_REVIEW → REJECTED`

---

# 5. Segurança e privacidade

Como o anonimato é uma das principais características da solução, o sistema deverá implementar mecanismos destinados a reduzir riscos de identificação dos denunciantes.

As medidas previstas incluem:

- Autenticação obrigatória para o registro de denúncias.
- Controle de acesso baseado nos perfis de usuário.
- Ausência de associação direta entre denúncias e contas dos moradores.
- Geração de protocolos imprevisíveis.
- Validação e tratamento das imagens enviadas.
- Proteção contra envios automatizados e tentativas abusivas de acesso.
- Restrição do acesso a dados administrativos internos.

---

# 6. Armazenamento de imagens

Para esta entrega backend, o armazenamento das imagens será validado com H2 nos testes, por meio da abstração de armazenamento. A implementação deve permitir envio e recuperação dos bytes sem depender de frontend ou banco externo. PostgreSQL fica como evolução opcional, caso haja tempo após a conclusão do backend.

A aplicação deverá utilizar uma interface de abstração de armazenamento, permitindo que futuramente seja implementada uma solução baseada em armazenamento de objetos na nuvem.

A estratégia inicial tem como objetivo simplificar o desenvolvimento do MVP, mantendo a possibilidade de evolução da infraestrutura.

---

# 7. Critérios de sucesso do MVP

O MVP backend será considerado funcional quando os seguintes cenários puderem ser executados com sucesso por requisições HTTP e testes de integração com H2. Menções a visualizar/acessar significam obter respostas de endpoints autorizados, sem exigir interface gráfica:

1. Um morador consegue realizar seu cadastro e autenticar-se na plataforma.
2. Um morador autorizado consegue registrar uma denúncia sem associação direta à sua identidade.
3. O sistema valida corretamente os dados obrigatórios da denúncia.
4. O morador consegue selecionar uma categoria e anexar imagens opcionalmente.
5. O sistema gera um protocolo após o registro.
6. O morador consegue consultar o andamento da denúncia utilizando suas credenciais de acompanhamento.
7. Um administrador consegue autenticar-se e acessar os endpoints administrativos.
8. O administrador consegue visualizar as denúncias do condomínio sob sua responsabilidade.
9. O administrador consegue consultar detalhes e imagens das denúncias.
10. O administrador consegue atualizar o status e registrar providências.
11. As alterações de status ficam registradas no histórico.
12. O morador consegue visualizar o andamento atualizado da denúncia.
13. As operações de registro e consulta não expõem diretamente a identidade do denunciante.

---

# 8. Possíveis evoluções futuras

Após a implementação e validação do MVP, o sistema poderá receber novas funcionalidades relacionadas à gestão condominial.

Entre as possibilidades estão:

- Frontend com telas de cadastro, denúncia, acompanhamento e gestão (opcional após o backend).
- Integração com PostgreSQL ou outro banco externo, incluindo migrações e provisionamento (opcional após o backend).

- Notificações automáticas sobre alterações de status.
- Dashboard administrativo com indicadores.
- Relatórios e estatísticas das denúncias.
- Gestão de manutenção e abertura de chamados.
- Reserva de áreas comuns.
- Controle financeiro condominial.
- Gestão de assembleias e votações.
- Comunicação entre moradores e administração.
- Aplicativo mobile nativo.
- Armazenamento de imagens em serviços de nuvem.
- Gerenciamento avançado de permissões administrativas.
