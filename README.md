# CHAMA Backend

API REST oficial do projeto **CHAMA**, construída em **Java 25**, **Spring Boot 4.2.0** e **PostgreSQL**, com arquitetura monolítica modular, controles rigorosos de cibersegurança (**OWASP API Top 10**) e conformidade com a **LGPD**.

---

## Stack Principal

- **Linguagem:** Java 25
- **Framework:** Spring Boot 4.2.0 (`webmvc`, `validation`, `actuator`, `data-jpa`, `flyway`, `security`, `oauth2-resource-server`)
- **Banco de Dados:** PostgreSQL 16+ / 18 (migrações versionadas via **Flyway**)
- **Build & Testes:** Apache Maven (`./mvnw`) + JUnit 5 + Mockito + MockMvc

---

## Documentação Completa (`docs/`)

Toda a documentação do projeto foi centralizada na pasta [`docs/`](./docs/README.md):

- [Índice Central da Documentação](./docs/README.md)
- [01 — Visão Geral e Como Funciona o Projeto](./docs/01-visao-geral-e-funcionamento.md)
- [02 — Regras de Negócio, Segurança e LGPD](./docs/02-regras-de-negocio.md)
- [03 — Mapa do Código (O que cada arquivo faz)](./docs/03-mapa-do-codigo.md)
- [04 — Fluxos do Sistema (Diagramas)](./docs/04-fluxos-do-sistema.md)
- [05 — Como Desenvolver Nesse Ambiente](./docs/05-ambiente-de-desenvolvimento.md)
- [06 — Guia de Contribuição para Colaboradores](./docs/06-guia-de-contribuicao.md)
- [07 — Guia do Setup Antigravity CLI](./docs/07-guia-antigravity.md)
- [Especificações Técnicas (Fonte da Verdade)](./docs/specs/README.md)

---

## Quickstart Local

1. **Copie o arquivo de variáveis de ambiente:**
   ```bash
   cp .env.example .env
   ```
2. **Prepare Java 25 e PostgreSQL** (no Fedora, há um script pronto; para outros sistemas ou Docker, veja o [Guia de Ambiente](./docs/05-ambiente-de-desenvolvimento.md)):
   ```bash
   sudo ./scripts/setup-fedora.sh
   ```
3. **Inicie a aplicação no perfil `dev`:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
4. **Verifique os healthchecks:**
   - `GET http://localhost:8080/health/live`
   - `GET http://localhost:8080/health/ready`

---

## Endpoints Disponíveis

| Método | Rota | Descrição | Autenticação |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Cadastra novo estudante e retorna tokens | Pública (Rate Limit) |
| `POST` | `/api/v1/auth/login` | Autentica usuário existente | Pública (Rate Limit) |
| `POST` | `/api/v1/auth/refresh` | Rotaciona `refresh_token` e renova sessão | Pública (Rate Limit) |
| `POST` | `/api/v1/auth/logout` | Revoga `refresh_token` (`204 No Content`) | Pública (Rate Limit) |
| `GET` | `/api/v1/auth/me` | Retorna perfil resumido do usuário logado | `Bearer <access_token>` |
| `GET` | `/health/live` | Verifica se o processo está ativo | Pública |
| `GET` | `/health/ready` | Verifica conectividade real com PostgreSQL | Pública |

Consulte exemplos de payloads e diagramas em [`docs/04-fluxos-do-sistema.md`](./docs/04-fluxos-do-sistema.md) e [`docs/specs/02-contrato-api.md`](./docs/specs/02-contrato-api.md).

---

## Testes Automatizados

```bash
./mvnw clean test
```

---

## Como Contribuir em Equipe

1. Atualize sua `main` local (`git checkout main && git pull --rebase origin main`).
2. Crie uma branch para sua tarefa (`git checkout -b feat/nome-da-feature`).
3. Leia as specs em `docs/specs/` antes de alterar contratos ou tabelas (coordene numeração de migrações Flyway `V2`, `V3`... com o time).
4. Rode `./mvnw clean test` e verifique que nenhum segredo (`.env`) ou PII entrou no código/logs.
5. Faça commits no padrão **Conventional Commits** e abra um **Pull Request** para revisão.

Leia as instruções completas em [`docs/06-guia-de-contribuicao.md`](./docs/06-guia-de-contribuicao.md).
