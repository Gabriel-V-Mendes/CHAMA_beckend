# Spec 05 — Padrões de Código, Git e Testes

## 1. Estilo de Código (Java 25 + Spring Boot 4.2)
- **Legibilidade:** Classes e métodos coesos, responsabilidade única, nomes descritivos e tipagem forte.
- **DTOs Imutáveis:** Use Java `record` para requests e responses, com validação Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Size`, etc.) na borda.
- **Injeção de Dependências:** Sempre por construtor explícito (evitar `@Autowired` em atributos/fields).
- **Comentários:** Enxutos, diretos e em Português (PT-BR), explicando o propósito de classes/métodos e o *porquê* de decisões de segurança/domínio não óbvias.

## 2. Estratégia de Git e Commits
- **Branches:** `main` sempre estável e deployável. Crie branches curtas por tarefa:
  - `feat/nome-da-feature`
  - `fix/descricao-do-bug`
  - `chore/ajuste-infra-ou-docs`
  - `sec/correcao-seguranca`
- **Conventional Commits:**
  - `feat(auth): adiciona endpoint de refresh token`
  - `fix(opportunities): corrige paginação quando lista está vazia`
  - `sec(users): bloqueia acesso IDOR em perfil de estudante`
  - `docs(specs): atualiza contrato de erro na spec 02`

## 3. Pirâmide de Testes (`./mvnw test`)
- **Unitários (Base):** Testam regras de negócio (`*ServiceTest`) de forma rápida e isolada com JUnit 5 + Mockito.
- **Integração:** Testam rotas HTTP + Spring Security + validação + Flyway + JPA (`*IntegrationTest`) com `MockMvc` e banco H2 em modo PostgreSQL.
- **Segurança/Regressão:** Todo controle crítico (Mass Assignment, rate limit, consentimento de menor, reuso de refresh token) e bug corrigido deve ter teste automatizado dedicado.

## 4. Definição de Pronto (DoD) para PRs
- [ ] Código compilando e seguindo os padrões do projeto sem warnings críticos
- [ ] Testes automatizados passando localmente (`./mvnw test`) e no CI
- [ ] Specs em `docs/specs/` atualizadas (se houve mudança de contrato/arquitetura/dados)
- [ ] Revisão realizada com `@.antigravity/skills/pr-review.md`
