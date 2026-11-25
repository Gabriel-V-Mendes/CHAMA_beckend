# Spec 04 — Segurança da Informação & Conformidade LGPD

## 1. Princípios Inegociáveis
O projeto CHAMA lida com dados de jovens e estudantes (muitos deles menores de 18 anos). Segurança e privacidade são requisitos funcionais de primeira classe.

## 2. Conformidade LGPD
- **Minimização de Dados:** Coletar apenas o estritamente necessário para conectar o jovem às oportunidades. Não pedir documentos sensíveis (CPF, RG) se não houver necessidade legal real.
- **Proteção a Menores (Art. 14 da LGPD):** Fluxos envolvendo menores devem prever linguagem clara e registro auditável de consentimento de responsável quando exigido.
- **Direitos do Titular:** Prever endpoints/rotinas para exportação de dados do usuário e exclusão/anonimização definitiva de conta.
- **Base Legal e Logs:** Registrar timestamp e versão dos Termos de Uso e Política de Privacidade aceitos.

## 3. Autenticação, Senhas e Sessões
- **Hash de Senhas:** Usar exclusivamente `Argon2id` (preferencial) ou `bcrypt` (custo >= 12). Nunca usar MD5/SHA para senhas.
- **JWT:** Access token com expiração curta (ex.: 15 min) e Refresh token rotacionável com revogação em banco/cache.
- **Segredos:** Assinar tokens com chaves fortes (mín. 256 bits) carregadas exclusivamente via variáveis de ambiente.

## 4. Proteção de API (OWASP API Top 10)
- **BOLA / IDOR:** Em toda rota com `/:id`, validar no service/repository que o recurso pertence ao `current_user.id` ou que o usuário possui role `admin`.
- **Rate Limiting:** Aplicar limite restrito em rotas sensíveis (`/auth/login`, `/auth/register`, `/auth/forgot-password`).
- **CORS:** Permitir apenas origens explícitas (domínios oficiais do frontend). Nunca usar `*` com `allow_credentials=True`.
- **Headers de Segurança:** Configurar `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security` (em HTTPS).

## 5. Logs e Observabilidade Segura
- **Mascaramento:** Nunca logar `Authorization`, cookies, senhas, tokens, e-mails completos ou datas de nascimento.
- **Rastreabilidade:** Incluir `request_id` em todos os logs para correlação sem expor dados pessoais.
