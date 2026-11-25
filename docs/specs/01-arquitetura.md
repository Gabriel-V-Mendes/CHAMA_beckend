# Spec 01 — Arquitetura do Sistema (CHAMA_beckend)

## 1. Contexto do Produto
O **CHAMA** é uma iniciativa social e educacional voltada a conectar jovens de escolas públicas e comunidades periféricas a oportunidades em tecnologia e inovação. O backend (`CHAMA_beckend`) centraliza autenticação, gestão de perfis, escolas/parceiros, oportunidades/trilhas e acompanhamento de inscrições.

## 2. Stack Tecnológica
- **Linguagem / Runtime:** Java 25
- **Framework:** Spring Boot 4.2.0 (`webmvc`, `validation`, `actuator`, `data-jpa`, `security`, `oauth2-resource-server`)
- **Banco de Dados:** PostgreSQL 16+ / 18
- **ORM / Migrações:** Spring Data JPA (Hibernate) + Flyway
- **Segurança:** Spring Security 7 (JWT HS256 via OAuth2 Resource Server + BCrypt custo >= 12)
- **Build:** Apache Maven (`./mvnw`)
- **Observabilidade:** Logs estruturados com correlação `request_id`, Healthchecks (`/health/live`, `/health/ready`, `/actuator/health`)

## 3. Estrutura de Camadas (Monolito Modular)
```text
src/main/java/com/chama/backend/
├── ChamaBackendApplication.java
├── core/                  # Infraestrutura transversal (config, security, exception)
│   ├── config/            # Properties tipadas e beans de configuração
│   ├── exception/         # Exceções de domínio e GlobalExceptionHandler (RFC 7807)
│   └── security/          # Filtros (RequestId, RateLimit), JWT e SecurityConfig
└── modules/               # Módulos por domínio (auth, user, health, ...)
    └── <dominio>/
        ├── controller/    # Camada HTTP REST e validação de entrada
        ├── dto/           # Records imutáveis de request/response
        ├── service/       # Regras de negócio e transações
        ├── repository/    # Interfaces Spring Data JPA
        └── model/         # Entidades JPA e enums de domínio
```

## 4. Requisitos Não-Funcionais
- **Disponibilidade:** Resposta rápida em redes móveis (payloads enxutos e paginados).
- **Segurança:** Conformidade estrita com LGPD e OWASP API Top 10.
- **Testabilidade:** Serviços desacoplados para testes unitários rápidos (Mockito) e testes de integração isolados (MockMvc + H2 em modo PostgreSQL + Flyway).

## 5. Registro de Decisões Arquiteturais (ADRs)
| ID | Data | Decisão | Motivo | Status |
| :--- | :--- | :--- | :--- | :--- |
| ADR-001 | 2026-11 | Arquitetura monolítica modular por domínio | Facilita entrega rápida, manutenção por time enxuto e evolução futura | Aprovado |
| ADR-002 | 2026-11 | PostgreSQL como banco relacional principal | Integridade referencial forte para usuários, escolas e inscrições | Aprovado |
| ADR-003 | 2026-11 | Autenticação Stateless JWT HS256 curta (15 min) + Refresh Token opaco rotacionável (hash SHA-256 em banco) | Evita sessões em servidor, permite revogação imediata e detecta reuso de refresh tokens | Aprovado |
