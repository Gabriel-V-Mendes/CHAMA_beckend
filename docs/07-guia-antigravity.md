# 07 — Guia Rápido do Setup Antigravity no CHAMA_beckend

Este repositório já inclui a estrutura de **Regras Globais, Agentes, Skills, Specs e modelos de MCP** em `.antigravityrules`, `.antigravity/` e `docs/specs/` para acelerar o trabalho com o **Antigravity CLI**.

---

## 1. Segurança Local (Para Cada Colaborador)

Verifique se o `.gitignore` ignora arquivos `.env*` locais (mantendo apenas `*.example`):
```gitignore
.env
.env.local
.env.*.local
.env.mcp
```

### Configurar Servidores MCP (Opcional)
1. Copie os servidores desejados de [`.antigravity/mcp/settings.example.json`](../.antigravity/mcp/settings.example.json) para sua configuração local do Antigravity/Gemini CLI (ex.: `~/.gemini/settings.json`).
2. Exporte eventuais tokens apenas no seu shell (`~/.bashrc` ou `~/.zshrc`) — **nunca** salve tokens reais no repositório:
   ```bash
   export GITHUB_TOKEN="seu_token_fine_grained"
   export DATABASE_READONLY_URL="postgresql://chama_readonly:senha@localhost:5432/chama"
   ```

---

## 2. Exemplos de Uso no Dia a Dia (Combos)

### Planejar uma nova feature (Tech Lead + Spec-First)
```text
@.antigravity/agents/techlead.md @.antigravity/skills/spec-first.md
Precisamos criar o módulo de cadastro e listagem de oportunidades em Java Spring.
```

### Implementar rápido economizando tokens (Backend + Caveman)
```text
@.antigravity/agents/backend.md @.antigravity/skills/caveman.md
Implemente GET /api/v1/opportunities com paginação seguindo @docs/specs/02-contrato-api.md
```

### Desenvolver guiado por testes (Backend + TDD)
```text
@.antigravity/agents/backend.md @.antigravity/skills/tdd-workflow.md
Crie a validação de inscrição duplicada em applications e valide com ./mvnw test.
```

### Auditoria de Segurança / LGPD antes do PR (Cybersecurity + Sec-Audit)
```text
@.antigravity/agents/cybersecurity.md @.antigravity/skills/sec-audit.md
Audite o módulo de autenticação verificando OWASP API Top 10 e @docs/specs/04-seguranca-lgpd.md
```

### Revisar o diff antes de abrir PR (Tech Lead + PR Review)
```text
@.antigravity/agents/techlead.md @.antigravity/skills/pr-review.md
Revise as alterações do meu branch atual antes de abrir o Pull Request.
```

### Investigar bug difícil (Backend + Root Cause)
```text
@.antigravity/agents/backend.md @.antigravity/skills/debug-root-cause.md
Investigue por que a validação de data de nascimento está falhando no fuso UTC.
```
