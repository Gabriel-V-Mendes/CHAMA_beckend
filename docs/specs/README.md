# Especificações do Projeto — CHAMA_beckend

Esta pasta é a **fonte da verdade técnica** do projeto CHAMA. Todos os colaboradores e agentes do Antigravity CLI consultam estes arquivos para manter consistência entre entregas.

> Voltar ao [Índice Geral da Documentação](../README.md).

## Índice
1. [`01-arquitetura.md`](./01-arquitetura.md) — Visão geral do sistema, stack, estrutura modular e ADRs.
2. [`02-contrato-api.md`](./02-contrato-api.md) — Convenções REST, autenticação, paginação e formato de erro.
3. [`03-modelo-dados.md`](./03-modelo-dados.md) — Entidades de domínio, relacionamentos, índices e migrações.
4. [`04-seguranca-lgpd.md`](./04-seguranca-lgpd.md) — Controles AppSec, proteção de dados pessoais e LGPD.
5. [`05-padroes-codigo.md`](./05-padroes-codigo.md) — Convenções de código, Git, PRs e estratégia de testes.

## Regra de Manutenção
Sempre que uma decisão arquitetural, endpoint ou tabela mudar, atualize a spec correspondente no mesmo PR.
