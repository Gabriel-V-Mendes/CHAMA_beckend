# CHAMA Backend

Repositório do backend do projeto **CHAMA**, estruturado em **Java 25** com **Spring Boot 4.2** e **Maven**.

## Stack

- **Linguagem:** Java 25
- **Framework:** Spring Boot 4.2.0 (`webmvc`, `validation`, `actuator`)
- **Build:** Maven Wrapper (`./mvnw`)

## Estrutura do Projeto

```text
CHAMA_beckend/
├── .env.example
├── .gitignore
├── mvnw / mvnw.cmd
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/chama/backend/
    │   │   ├── ChamaBackendApplication.java
    │   │   ├── config/          # Configurações da aplicação (CORS, segurança, beans)
    │   │   ├── controller/      # Endpoints REST (ex.: HealthController)
    │   │   ├── dto/             # Objetos de entrada e saída (request/response)
    │   │   ├── exception/       # Exceções customizadas e GlobalExceptionHandler
    │   │   ├── model/           # Entidades / modelos de domínio
    │   │   ├── repository/      # Camada de acesso a dados
    │   │   └── service/         # Regras de negócio
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/chama/backend/
            ├── ChamaBackendApplicationTests.java
            └── controller/
                └── HealthControllerTest.java
```

## Como executar

1. Clone o repositório e entre na pasta:

```bash
git clone https://github.com/Gabriel-V-Mendes/CHAMA_beckend.git
cd CHAMA_beckend
```

2. (Opcional) Copie o arquivo de variáveis de ambiente de exemplo:

```bash
cp .env.example .env
```

3. Inicie a aplicação com o Maven Wrapper (não precisa ter Maven instalado globalmente):

```bash
./mvnw spring-boot:run
```

4. Verifique se está rodando:

- Health check customizado: `GET http://localhost:8080/api/health`
- Actuator health: `GET http://localhost:8080/actuator/health`

## Testes

```bash
./mvnw test
```
