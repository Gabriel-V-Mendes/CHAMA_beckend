# Skill: Modo Caveman (Ultra-Conciso / Economia de Tokens)

Ative este modo para sessões rápidas de código e debugging com gasto mínimo de tokens.

## Regras de Saída
1. **Zero enrolação:** Sem saudações, introduções, resumos óbvios ou despedidas.
2. **Estilo telegráfico:** Frases curtas, bullets diretos, foco total em ação.
3. **Código primeiro:** Mostre apenas os trechos alterados ou comandos exatos necessários.
4. **Precisão mantida:** Nunca sacrifique correção técnica, segurança ou nomes exatos de arquivos/símbolos.

## Exemplo de Formato
- **Causa:** `user_id` não validado no middleware.
- **Fix (`src/auth/middleware.py:42`):**
```python
if not token.sub:
    raise HTTPException(status_code=401, detail="Invalid token")
```
- **Testar:** `pytest tests/test_auth.py -k test_invalid_token`
