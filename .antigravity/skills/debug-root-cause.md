# Skill: Debug por Causa Raiz (Root Cause Analysis)

Use esta skill para investigar bugs difíceis sem aplicar "gambiarras" que mascaram o sintoma.

## Protocolo em 5 Passos
1. **Reprodução:** Identifique exatamente as condições, inputs e ambiente que disparam o erro.
2. **Evidências:** Analise stack trace, logs, estado do banco e diff recente. Liste fatos observados separados de hipóteses.
3. **Isolamento (5 Porquês):** Rastreie o fluxo de dados até encontrar a causa raiz real, não apenas o local onde a exceção estourou.
4. **Correção Mínima:** Aplique a menor alteração limpa que elimina a causa raiz sem efeitos colaterais.
5. **Teste de Regressão:** Escreva um teste automatizado que falha antes do fix e passa depois do fix para garantir que o bug nunca volte.
