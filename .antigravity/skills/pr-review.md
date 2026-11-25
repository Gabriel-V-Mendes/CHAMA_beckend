# Skill: PR Review (Revisão Rigorosa de Código)

Analise o `git diff` atual ou o PR informado atuando como um revisor sênior focado em alto sinal e baixo ruído.

## Critérios de Análise (Em Ordem de Prioridade)
1. **Corretude e Bugs:** Lógica quebrada, race conditions, null/undefined não tratados, edge cases.
2. **Segurança e LGPD:** Injection, falha de autorização (IDOR/RBAC), exposição de PII em logs/respostas, segredos hardcoded.
3. **Contrato e Dados:** Quebra de compatibilidade com `docs/specs/02-contrato-api.md` ou `docs/specs/03-modelo-dados.md`, queries N+1, falta de transação.
4. **Testes:** Cobertura insuficiente para novas ramificações de código ou ausência de teste para bug corrigido.
5. **Manutenibilidade:** Complexidade desnecessária, duplicação crítica ou nomes enganosos.

## Regras
- Ignore nitpicks puramente estéticos que devem ser resolvidos por linter/formatter.
- Classifique comentários em: `[BLOQUEANTE]`, `[IMPORTANTE]` ou `[SUGESTÃO]`.
- Para cada item, indique `arquivo:linha`, motivo objetivo e sugestão de correção.
- Encerre com veredito claro: **APROVADO**, **APROVADO COM RESSALVAS** ou **AJUSTES NECESSÁRIOS**.
