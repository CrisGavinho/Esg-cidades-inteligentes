# Projeto - Cidades ESG Inteligentes

> Aplicação Java Spring Boot para monitoramento de indicadores ESG (Environmental, Social and Governance) em cidades inteligentes, com pipeline completo de CI/CD e containerização Docker.

**Stack:** Java 21 · Spring Boot 3.2 · SQL Server 2022 · Docker · GitHub Actions

**Grupo:** Bruno Caffé (RM560021) · Bernardo Rosiano (RM560291) · Caio Veríssimo (RM560558) · Cristiana Gavinho (RM559927) · Danilo França (RM560670)

---

## Como executar localmente com Docker

### Pré-requisitos
- Docker 24+ instalado
- Docker Compose v2+

### 1. Clone o repositório e configure o ambiente

```bash
git clone https://github.com/CrisGavinho/esg-cidades-inteligentes.git
cd esg-cidades-inteligentes

cp .env.example .env
# Edite o .env com suas senhas se desejar
```

Conteúdo de referência do `.env.example`:

```env
DB_NAME=esg_staging
DB_PASSWORD=Esg@Staging2024
DB_NAME_PROD=esg_prod
DB_PASSWORD_PROD=SENHA_FORTE
DOCKER_IMAGE=esg-cidades-inteligentes
IMAGE_TAG=latest
```

### 2. Subir o ambiente Staging

```bash
docker compose --profile staging up -d
```

A aplicação ficará disponível em: **http://localhost:8080**

### 3. Subir o ambiente Produção

```bash
docker compose --profile prod up -d
```

A aplicação ficará disponível em: **http://localhost:8081**

### 4. Verificar saúde da aplicação

```bash
curl http://localhost:8080/actuator/health
```

### 5. Exemplos de uso da API

```bash
# Listar todos os indicadores
curl http://localhost:8080/api/indicadores

# Criar um indicador
curl -X POST http://localhost:8080/api/indicadores \
  -H "Content-Type: application/json" \
  -d '{
    "cidade": "São Paulo",
    "categoria": "AMBIENTAL",
    "indicador": "Emissão de CO2",
    "valor": 120.5,
    "unidade": "toneladas",
    "ano": 2024
  }'

# Buscar por cidade
curl http://localhost:8080/api/indicadores/cidade/São Paulo
```

### 6. Parar os containers

```bash
docker compose --profile staging down
docker compose --profile prod down
```

### Serviços disponíveis

| Serviço           | Profile | Porta | Descrição                     |
|-------------------|---------|-------|-------------------------------|
| sqlserver-staging | staging | 1433  | SQL Server 2022 para staging  |
| app-staging       | staging | 8080  | API Spring Boot — staging     |
| sqlserver-prod    | prod    | 1434  | SQL Server 2022 para produção |
| app-prod          | prod    | 8081  | API Spring Boot — produção    |

> **Nota para Mac com Apple Silicon (ARM):** A imagem oficial do SQL Server não suporta ARM64. Utilize a imagem `azure-sql-edge` para desenvolvimento local em chips Apple Silicon.

---

## Pipeline CI/CD

### Ferramenta utilizada
**GitHub Actions** — integrado nativamente ao repositório, sem infraestrutura adicional.

### Fluxo do Pipeline

```
Push → Build & Testes → Docker Build → Deploy Staging/Prod
```

| Branch    | Ambientes ativados         |
|-----------|---------------------------|
| `develop` | Build → Testes → Staging  |
| `main`    | Build → Testes → Produção |
| PR        | Build → Testes (somente)  |

### Etapas detalhadas

**1. Build & Testes** (`build-and-test`)
- Configura Java 21 com cache de dependências Maven
- Executa `mvn clean verify` — compila e roda todos os testes
- Publica relatório de testes como artefato

**2. Build da Imagem Docker** (`docker-build`)
- Executa apenas em pushes (não em PRs)
- Faz login no GitHub Container Registry (GHCR)
- Constrói e publica a imagem com tags automáticas (`latest`, `sha-*`, nome do branch)
- Usa cache de camadas para builds mais rápidos

**3. Deploy Staging** (`deploy-staging`)
- Ativado apenas na branch `develop`
- Conecta ao servidor via SSH usando `appleboy/ssh-action@v1.0.3`
- Atualiza a imagem e reinicia os containers com `docker compose`
- Realiza health check em `/actuator/health` após o deploy

**4. Deploy Produção** (`deploy-production`)
- Ativado apenas na branch `main`
- Requer aprovação manual configurada nos Environments do GitHub
- Mesmo fluxo do staging, apontando para servidor de produção

### Trecho do `ci-cd.yml`

```yaml
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', cache: maven }
      - run: mvn clean verify -B

  deploy-staging:
    needs: docker-build
    if: github.ref == 'refs/heads/develop'
    environment: staging
    steps:
      - uses: appleboy/ssh-action@v1.0.3
```

### Secrets necessários no GitHub

| Secret | Descrição |
|--------|-----------|
| `STAGING_HOST` | IP ou domínio do servidor staging |
| `STAGING_USER` | Usuário SSH staging |
| `STAGING_SSH_KEY` | Chave privada SSH staging |
| `STAGING_DB_PASSWORD` | Senha do SQL Server staging |
| `PROD_HOST` | IP ou domínio do servidor produção |
| `PROD_USER` | Usuário SSH produção |
| `PROD_SSH_KEY` | Chave privada SSH produção |
| `PROD_DB_PASSWORD` | Senha do SQL Server produção |

---

## Containerização

### Dockerfile — Multi-stage Build

```dockerfile
# Stage 1: Build com Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B   # cache de dependências
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime mínimo com JRE Alpine
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S esggroup && adduser -S esguser -G esggroup
COPY --from=builder /app/target/*.jar app.jar
RUN chown esguser:esggroup app.jar
USER esguser
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:staging}", "app.jar"]
```

### Estratégias adotadas

- **Multi-stage build:** separa o ambiente de build do runtime, reduzindo o tamanho final da imagem (~200MB vs ~700MB)
- **Cache de dependências:** `dependency:go-offline` em camada separada evita redownload em cada build
- **Usuário não-root:** criação de usuário `esguser` para maior segurança
- **JRE Alpine:** imagem base mínima (~85MB) em vez do JDK completo
- **Health check integrado:** o Docker Compose aguarda a aplicação estar saudável antes de considerar o container como pronto
- **Profile por variável de ambiente:** `SPRING_PROFILES_ACTIVE` controla staging vs prod sem rebuild de imagem

---

## Prints do funcionamento

Evidências reais de execução do pipeline no repositório `CrisGavinho/Esg-cidades-inteligentes`.

### Pipeline GitHub Actions
- **Print 1** — Diagrama do pipeline CI/CD com as etapas encadeadas: Build → Docker → Deploy
- **Print 2** — Job Build & Testes concluído com sucesso (~5 minutos, todos os 6 testes passando)
- **Print 3** — Build e push da imagem Docker no GHCR (~1 min 19s)
- **Print 4** — Imagem publicada em `ghcr.io/crisgavinho/esg-cidades-inteligentes`

### Testes automatizados executados no pipeline

| Teste | Descrição |
|-------|-----------|
| `deveListarTodos` | Verifica retorno de lista com todos os indicadores |
| `deveBuscarPorId` | Busca indicador por ID existente — retorna Optional preenchido |
| `deveRetornarVazioParaIdInexistente` | ID inexistente — retorna Optional vazio |
| `deveSalvarIndicador` | Persiste entidade e verifica chamada ao repositório |
| `deveDeletarPorId` | Deleta por ID e verifica interação com repositório |
| `deveBuscarPorCidade` | Filtra indicadores por cidade |

### Ambientes em execução
- **Print 5** — `docker compose ps` mostrando containers ativos (staging)
- **Print 6** — Resposta do `/actuator/health` em staging
- **Print 7** — Resposta do `/actuator/health` em produção
- **Print 8** — Requisição à API `/api/indicadores` retornando dados

> Os prints completos estão disponíveis na documentação técnica em PDF entregue junto ao projeto.

---

## Tecnologias utilizadas

| Categoria | Tecnologia |
|-----------|------------|
| Linguagem | Java 21 (Eclipse Temurin) |
| Framework | Spring Boot 3.2.5 |
| Banco de dados | SQL Server 2022 |
| ORM | Spring Data JPA / Hibernate |
| Testes | JUnit 5, Mockito, H2 (in-memory) |
| Cobertura | JaCoCo |
| Containerização | Docker 24, Docker Compose v2 |
| CI/CD | GitHub Actions |
| Registry | GitHub Container Registry (GHCR) |
| Build tool | Maven 3.9.6 |
| Deploy SSH | appleboy/ssh-action v1.0.3 |
| Monitoramento | Spring Actuator (`/actuator/health`) |

---

## Checklist de Entrega

| Item | Status |
|------|--------|
| Projeto compactado em .ZIP com estrutura organizada | ✅ |
| Dockerfile funcional (multi-stage) | ✅ |
| docker-compose.yml com staging e produção (profiles) | ✅ |
| Pipeline com etapas de build, teste e deploy | ✅ |
| README.md com instruções e prints | ✅ |
| Documentação técnica com evidências (PDF) | ✅ |
| Deploy realizado nos ambientes staging e produção | ✅ |
