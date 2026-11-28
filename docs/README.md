# Documentação do Projeto — CHAMA Backend

Bem-vindo à central de documentação do **CHAMA_beckend**. Aqui está tudo o que qualquer pessoa colaboradora precisa para entender o produto, a arquitetura, as regras de negócio, o papel de cada arquivo, o setup do ambiente e o fluxo de contribuição em equipe.

---

## 1. Guias do Projeto e da Equipe

| Documento | O que você encontra |
| :--- | :--- |
| [01 — Visão Geral e Funcionamento](./01-visao-geral-e-funcionamento.md) | Propósito do CHAMA, stack Java 25 + Spring Boot 4.2 + PostgreSQL, arquitetura modular e caminho de uma requisição |
| [02 — Regras de Negócio, Segurança e LGPD](./02-regras-de-negocio.md) | Regras de cadastro, login, proteção a menores (Art. 14 LGPD), tokens JWT/refresh, rate limit e RBAC |
| [03 — Mapa do Código](./03-mapa-do-codigo.md) | Explicação direta, arquivo por arquivo, do que cada classe, configuração, migração e teste faz |
| [04 — Fluxos do Sistema](./04-fluxos-do-sistema.md) | Diagramas de sequência e passo a passo de cadastro, login, acesso autenticado, refresh, logout e erros |
| [05 — Ambiente de Desenvolvimento](./05-ambiente-de-desenvolvimento.md) | Setup no Fedora, Ubuntu, macOS/Windows ou Docker, perfis `dev`/`test`, criação de módulos e migrações |
| [06 — Guia de Contribuição](./06-guia-de-contribuicao.md) | Fluxo Git para múltiplos colaboradores, branches, Conventional Commits, PRs e coordenação de migrações |
| [07 — Guia do Antigravity CLI](./07-guia-antigravity.md) | Como usar agentes, skills e MCPs configurados em `.antigravity/` durante o desenvolvimento |

---

## 2. Especificações Técnicas (Fonte da Verdade)

Antes de criar novos endpoints, alterar contratos JSON ou mexer no banco de dados, consulte e mantenha atualizadas as specs em [`docs/specs/`](./specs/README.md):

1. [Spec 01 — Arquitetura](./specs/01-arquitetura.md)
2. [Spec 02 — Contrato de API](./specs/02-contrato-api.md)
3. [Spec 03 — Modelo de Dados](./specs/03-modelo-dados.md)
4. [Spec 04 — Segurança e LGPD](./specs/04-seguranca-lgpd.md)
5. [Spec 05 — Padrões de Código, Git e Testes](./specs/05-padroes-codigo.md)
6. [Spec 06 — Modelo de Dados: Eventos](./specs/06-modelo-dados-eventos.md)

---

## 3. Banco de Dados e Rollback

- Migrações versionadas pelo Flyway: [`src/main/resources/db/migration/`](../src/main/resources/db/migration/)
- Scripts manuais de reversão: [`docs/db/rollback/`](./db/rollback/)
- Diagramas ER: [`docs/diagrams/`](./diagrams/)
