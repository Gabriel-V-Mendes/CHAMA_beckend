# 05 — Como Desenvolver Nesse Ambiente

Guia prático para configurar sua máquina, rodar a aplicação localmente, executar testes e evoluir módulos e banco de dados.

---

## 1. Pré-requisitos

- **Java JDK 25** (com `javac` disponível; apenas o JRE não compila o projeto)
- **PostgreSQL 16+ ou 18** (nativo ou via Docker)
- **Maven:** opcional ter instalado globalmente, pois o repositório inclui o Maven Wrapper (`./mvnw` / `mvnw.cmd`)
- **Git** configurado com sua conta do GitHub

---

## 2. Setup Inicial por Sistema Operacional

### Opção A — Fedora / RHEL (Automatizado)
O projeto possui um script idempotente que instala `java-25-openjdk-devel`, `maven`, `postgresql-server`, configura `scram-sha-256` no `pg_hba.conf`, inicia o serviço e cria o banco `chama` com usuário `chama_app`:

```bash
sudo ./scripts/setup-fedora.sh
```

### Opção B — Ubuntu / Debian
```bash
# Instale JDK 25 (ou via SDKMAN) e PostgreSQL
sudo apt update && sudo apt install -y postgresql postgresql-contrib
curl -s "https://get.sdkman.io" | bash && source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-tem

# Crie usuário e banco locais
sudo -u postgres psql -c "CREATE ROLE chama_app WITH LOGIN PASSWORD 'chama_dev_local_2026';"
sudo -u postgres psql -c "CREATE DATABASE chama OWNER chama_app;"
```

### Opção C — macOS / Windows
- **macOS:** `brew install openjdk@25 postgresql@17` (ou SDKMAN para Java 25) e crie o role `chama_app` + database `chama`.
- **Windows:** instale o JDK 25 (Adoptium/Temurin) e PostgreSQL 16+ pelo instalador oficial ou `winget`, e crie o role `chama_app` + database `chama`. Use `.\mvnw.cmd` no PowerShell.

### Opção D — Banco Rápido via Docker (Qualquer SO)
Se não quiser instalar PostgreSQL nativo na máquina:
```bash
docker run -d --name chama-postgres \
  -e POSTGRES_DB=chama \
  -e POSTGRES_USER=chama_app \
  -e POSTGRES_PASSWORD=chama_dev_local_2026 \
  -p 5432:5432 \
  postgres:17-alpine
```

---

## 3. Configurando Variáveis de Ambiente e Rodando

1. Copie o arquivo de exemplo (o `.env` é ignorado pelo Git):
   ```bash
   cp .env.example .env
   ```
2. Para desenvolvimento local rápido, ative o perfil `dev` (que preenche defaults locais caso `DB_PASSWORD`/`JWT_SECRET` não estejam exportados no shell):
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   *(Ou carregando o `.env` no shell antes de rodar sem perfil extra.)*

3. Confirme que subiu corretamente:
   - Liveness: `curl http://localhost:8080/health/live`
   - Readiness (testa conexão real no PostgreSQL): `curl http://localhost:8080/health/ready`

---

## 4. Executando os Testes

Os testes usam o perfil `test` (`application-test.properties`) com **H2 em memória no modo PostgreSQL** e **Flyway ativo**, então rodam de forma isolada mesmo sem PostgreSQL ligado:

```bash
./mvnw clean test
```

---

## 5. Como Evoluir o Projeto (Novos Módulos e Migrações)

### Criando um Novo Módulo (ex.: `opportunities`)
1. Consulte primeiro `docs/specs/` (Spec-First).
2. Crie o pacote `src/main/java/com/chama/backend/modules/<dominio>/` com:
   - `model/`, `repository/`, `dto/` (Java `record`), `service/`, `controller/`.
3. Adicione rotas públicas ou protegidas em `SecurityConfig.java` quando necessário (padrão seguro: tudo protegido por JWT salvo exceções explícitas).
4. Escreva testes unitários (`*ServiceTest.java`) e de integração (`*IntegrationTest.java`).

### Criando uma Nova Migração Flyway
1. Verifique o último número `V{N}` em `src/main/resources/db/migration/` (atualmente `V1`).
2. Crie `src/main/resources/db/migration/V2__descricao_curta.sql` usando SQL compatível com PostgreSQL (e H2 `MODE=PostgreSQL` para manter a suíte de testes verde).
3. Crie o script correspondente de reversão em `docs/db/rollback/U2__rollback_descricao_curta.sql`.
4. Atualize `docs/specs/03-modelo-dados.md`.

---

## 6. Solução Rápida de Problemas Comuns

| Sintoma | Causa Provável | Como Resolver |
| :--- | :--- | :--- |
| `Ident authentication failed for user "chama_app"` | `pg_hba.conf` do Linux está usando `ident` em `127.0.0.1/32` | Troque `ident` por `scram-sha-256` em `pg_hba.conf` e execute `sudo systemctl reload postgresql` (ou rode `sudo ./scripts/setup-fedora.sh`). |
| `javac: comando não encontrado` ou erro de release 25 | Apenas o JRE está instalado, sem JDK 25 | Instale `java-25-openjdk-devel` (Fedora) ou JDK 25 completo e confira `javac -version`. |
| Falha na subida por `AppSecurityProperties` | `JWT_SECRET` ausente ou menor que 32 caracteres | Rode com `-Dspring-boot.run.profiles=dev` ou exporte `JWT_SECRET` com no mínimo 32 caracteres. |
| Porta `8080` já está em uso | Outro serviço rodando na porta | Execute com `SERVER_PORT=8081 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. |
