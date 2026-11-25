# Skill: Auditoria de Segurança & Pentest Defensivo (OWASP + LGPD)

Execute uma auditoria sistemática de segurança no módulo, endpoint ou repositório indicado.

## Checklist de Verificação
1. **Autenticação & Sessão:** Expiração de JWT, validação de assinatura/algoritmo, proteção contra brute force, armazenamento seguro de senhas.
2. **Autorização (BOLA/IDOR & RBAC):** Todo recurso valida se o usuário autenticado tem permissão para ler/alterar aquele ID específico?
3. **Validação de Entrada:** SQL Injection, NoSQL Injection, Command Injection, Path Traversal, Mass Assignment (campos extras indevidos no body).
4. **Exposição de Dados (LGPD):** Campos sensíveis (senha, hash, CPF, dados de menores) filtrados nos serializers/DTOs? Logs livres de PII?
5. **Configuração & Infra:** CORS restritivo, headers de segurança (HSTS, CSP, X-Content-Type-Options), rate limiting, debug desativado em produção.
6. **Dependências & Segredos:** Busca por chaves hardcoded, `.env` exposto, pacotes com CVEs conhecidos.

## Formato de Saída
1. **Resumo de Risco** (Tabela com contagem por severidade: Crítica / Alta / Média / Baixa).
2. **Detalhamento dos Achados** (`arquivo:linha`, risco real, patch exato).
3. **Plano de Correção Priorizado** (o que corrigir imediatamente antes do deploy).
