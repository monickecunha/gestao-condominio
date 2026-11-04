# Kanban — MVP backend · entrega em 16/11/2026

[Quadro no GitHub Projects](https://github.com/users/monickecunha/projects/4)

## Escopo revisado em 02/11/2026

Entrega somente **backend/API**, com **H2 para testes**. Modelagem e persistência via repositórios continuam no backend. Frontend e PostgreSQL/banco externo ficam opcionais, apenas após backend validado e se houver tempo (K20/K21). O perfil H2 em memória para demonstração local é uma proposta em K02, a confirmar em K01; documentar que os dados são temporários.

Base: [planejamento](planning.md), [regras](businessRules.md), [histórias](userStories.md). Equipe: quatro pessoas. Nomes e disponibilidade diária ainda precisam ser definidos. Os cartões K11–K13 foram replanejados: saem telas e entram contrato HTTP e testes de integração.

## Fluxo e conclusão

Backlog → Ready → In progress (limite 4, idealmente um cartão por pessoa) → In review → Done.

K01 e K02 estão prontos para iniciar. K01 tem decisões documentadas marcadas, mas permanece aberta para dúvidas reais. Conclusão de desenvolvimento exige revisão, integração, critérios atendidos e evidências de testes. K01 encerra quando as decisões pendentes estiverem documentadas, sem exigir implementar o sistema. Bloqueios são registrados nas issues. Reunião diária de 10 minutos e revisão diária de PRs.

## Marcos propostos

- Até 03/11: decisões, ambiente backend/H2 e contrato inicial.
- Até 06/11: entidades/repositórios, autenticação, cadastros/aprovação e denúncia.
- Até 09/11: fluxo HTTP completo integrado e testes de morador/administração.
- Até 12/11: imagens, filtros, outros moradores, privacidade; congelar funcionalidades.
- 13–14/11: homologação do backend e correções com H2.
- 15/11: ensaio via coleção HTTP e ambiente local reproduzível.
- 16/11: entrega do backend.

Datas incluem fins de semana e são ajustáveis à disponibilidade. Opcionais K20/K21 não têm prazo e não bloqueiam o MVP. P1 continua obrigatório no backend; P2 é opcional.

## Quatro frentes de backend

- A: ambiente/H2, autenticação, segurança e entrega (K02, K04, K17, K19).
- B: modelagem, condomínio/unidades, vínculos, testes do morador e outros moradores (K03, K05, K06, K12, K16).
- C: denúncias, protocolo, acompanhamento e imagens (K07, K08, K14); apoio à modelagem inicial.
- D: contrato HTTP, gestão administrativa, testes administrativos e filtros (K11, K09, K10, K13, K15).
- Todos: decisões K01, revisão e homologação K18. A/C apoiam B conforme gargalos.

## Cartões

Dependências são descritas nas issues; não são relações nativas do GitHub. Datas e prioridades também constam dos campos do Projects.

| Cartão | Prioridade | Meta | Frente | Dependências |
|---|---|---|---|---|
| [K01 — Consolidar decisões do backend e resolver pendências da API](https://github.com/monickecunha/gestao-condominio/issues/1) | P0 | 03/11 | Equipe | Nenhuma |
| [K02 — Preparar backend, H2 para testes e build](https://github.com/monickecunha/gestao-condominio/issues/2) | P0 | 03/11 | A | Nenhuma |
| [K03 — Modelar entidades e validar persistência com H2](https://github.com/monickecunha/gestao-condominio/issues/3) | P0 | 04/11 | B | K01, K02 |
| [K04 — Implementar autenticação e autorização](https://github.com/monickecunha/gestao-condominio/issues/4) | P0 | 05/11 | A | K01, K02, K03 |
| [K05 — Cadastrar condomínio, síndico e unidades](https://github.com/monickecunha/gestao-condominio/issues/5) | P0 | 05/11 | B | K03, K04 |
| [K06 — Solicitar cadastro e aprovar/rejeitar vínculo](https://github.com/monickecunha/gestao-condominio/issues/6) | P0 | 06/11 | B | K04, K05 |
| [K07 — Registrar denúncia anônima e gerar protocolo](https://github.com/monickecunha/gestao-condominio/issues/7) | P0 | 06/11 | C | K03, K04, K05, K06 |
| [K08 — Consultar andamento pelo protocolo](https://github.com/monickecunha/gestao-condominio/issues/8) | P0 | 07/11 | C | K01, K07 |
| [K09 — Listar denúncias e detalhes administrativos](https://github.com/monickecunha/gestao-condominio/issues/9) | P0 | 07/11 | D | K04, K07 |
| [K10 — Atualizar status, histórico e providências](https://github.com/monickecunha/gestao-condominio/issues/10) | P0 | 08/11 | D | K09 |
| [K11 — Documentar contrato HTTP e exemplos da API](https://github.com/monickecunha/gestao-condominio/issues/11) | P0 | 06/11 | D | K01; atualização contínua conforme K04–K10, K14–K16 |
| [K12 — Testar fluxo do morador via API com H2](https://github.com/monickecunha/gestao-condominio/issues/12) | P0 | 08/11 | B | K02, K04–K08; contrato K11 |
| [K13 — Testar fluxo administrativo integrado via API](https://github.com/monickecunha/gestao-condominio/issues/13) | P0 | 09/11 | D | K02, K09, K10; contrato K11 |
| [K14 — Implementar upload e recuperação de imagens na API](https://github.com/monickecunha/gestao-condominio/issues/14) | P1 | 11/11 | C | K03, K07, K09 |
| [K15 — Filtrar denúncias por categoria, status e período](https://github.com/monickecunha/gestao-condominio/issues/15) | P1 | 11/11 | D | K09 |
| [K16 — Cadastrar outros moradores da mesma unidade](https://github.com/monickecunha/gestao-condominio/issues/16) | P1 | 12/11 | B | K01, K06 |
| [K17 — Validar privacidade e proteção contra abuso](https://github.com/monickecunha/gestao-condominio/issues/17) | P0 | 12/11 | A | K04, K07, K08, K10, K14 |
| [K18 — Homologar backend e corrigir falhas](https://github.com/monickecunha/gestao-condominio/issues/18) | P0 | 14/11 | Equipe | K04–K17 |
| [K19 — Preparar entrega e demonstração do backend](https://github.com/monickecunha/gestao-condominio/issues/19) | P0 | 15/11 | A + Equipe | K02; conclusão: K18 |
| [K20 — Opcional: frontend após conclusão do backend](https://github.com/monickecunha/gestao-condominio/issues/20) | P2 | Sem prazo | A definir | K18, K19; somente se houver tempo |
| [K21 — Opcional: integrar PostgreSQL ou banco externo](https://github.com/monickecunha/gestao-condominio/issues/21) | P2 | Sem prazo | A definir | K18, K19; somente se houver tempo |

## Resultado da revisão da K01

Já definidos: dois perfis, aprovação pelo síndico, autorização para denunciar, anonimato, vínculo ao condomínio, protocolo imprevisível, restrição de dados internos, campos/categorias, status/transições e abstração de imagens. Backend somente e H2 para testes são decisões novas da responsável em 02/11/2026.

Ainda pendentes: mecanismo de autenticação, campos/endpoints, criação de unidades e identificação do responsável, login/escopo na consulta por protocolo, campos públicos/internos exatos, limites de imagens, confirmação do ambiente de demonstração e divisão nominal da equipe. Não marcar funcionalidades como implementadas com base apenas na documentação.
