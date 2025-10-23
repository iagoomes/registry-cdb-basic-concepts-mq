# CDB Registry - RabbitMQ Basic Concepts

Este projeto demonstra o fluxo básico de mensageria com RabbitMQ em um contexto de renda fixa (CDB).

## Arquitetura Multi-Módulo Maven

O projeto utiliza uma estrutura **multi-módulo Maven** (também conhecida como Maven Multi-Module ou Maven Reactor). Esta é uma abordagem profissional para organizar projetos com múltiplas aplicações ou componentes relacionados.

### Por que Multi-Módulo?

#### 1. **Organização e Separação de Responsabilidades**
Ao invés de ter tudo em um único projeto monolítico, separamos:
- **Producer API**: Responsável apenas por receber requisições HTTP e publicar mensagens
- **Consumer API**: Responsável apenas por consumir e processar mensagens

Cada módulo tem seu próprio ciclo de vida, dependências e configurações específicas.

#### 2. **Reutilização de Configurações**
O **POM Pai** (`pom.xml` na raiz) centraliza:
- Versões de dependências (`dependencyManagement`)
- Configurações de plugins (`pluginManagement`)
- Propriedades do projeto (Java version, encoding, etc.)

Isso evita duplicação e garante consistência entre os módulos.

#### 3. **Build Unificado**
Com um único comando `mvn clean install` na raiz, o Maven:
1. Identifica todos os módulos declarados no POM pai
2. Determina a ordem de build (Reactor Order)
3. Compila todos os módulos na sequência correta

#### 4. **Facilita Deploy Independente**
- Cada módulo gera seu próprio JAR executável
- Você pode fazer deploy apenas do módulo que mudou
- Cada módulo pode ter seu próprio Dockerfile otimizado

#### 5. **Isolamento de Dependências**
- Producer API precisa de `spring-boot-starter-web` (para REST)
- Consumer API **não precisa** de Web (só messaging)
- Cada um declara apenas o que realmente usa

### Estrutura do Projeto

```
registry-cdb-basic-concepts-mq/          ← Projeto Pai
├── pom.xml                              ← POM Pai (packaging: pom)
│   ├── <modules>
│   │   ├── producer-api                 ← Declaração dos módulos
│   │   └── consumer-api
│   ├── <dependencyManagement>           ← Versões centralizadas
│   └── <pluginManagement>               ← Configurações de plugins
│
├── producer-api/                        ← Módulo 1
│   ├── pom.xml                          ← POM do módulo (parent: pom pai)
│   ├── src/
│   ├── Dockerfile
│   └── target/                          ← JAR independente gerado
│       └── producer-api-0.0.1-SNAPSHOT.jar
│
└── consumer-api/                        ← Módulo 2
    ├── pom.xml                          ← POM do módulo (parent: pom pai)
    ├── src/
    ├── Dockerfile
    └── target/                          ← JAR independente gerado
        └── consumer-api-0.0.1-SNAPSHOT.jar
```

### Como Funciona o POM Pai

#### POM Pai (`pom.xml` na raiz)
```xml
<packaging>pom</packaging>  <!-- NÃO gera JAR, apenas coordena -->

<modules>
    <module>producer-api</module>  <!-- Lista de módulos -->
    <module>consumer-api</module>
</modules>

<dependencyManagement>  <!-- Define versões, mas NÃO adiciona dependências -->
    <dependencies>
        <dependency>
            <groupId>org.openapitools</groupId>
            <artifactId>jackson-databind-nullable</artifactId>
            <version>0.2.1</version>  <!-- Versão centralizada -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### POM dos Módulos
```xml
<parent>  <!-- Herda configurações do pai -->
    <groupId>br.com.iagoomes</groupId>
    <artifactId>registry-cdb-basic-concepts-mq</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>

<artifactId>producer-api</artifactId>  <!-- Apenas artifactId (groupId e version herdados) -->

<dependencies>
    <dependency>
        <groupId>org.openapitools</groupId>
        <artifactId>jackson-databind-nullable</artifactId>
        <!-- SEM versão! Usa a do pai -->
    </dependency>
</dependencies>
```

### Vantagens para Este Projeto

1. **Simula Arquitetura Real**: Em produção, producer e consumer geralmente são serviços separados
2. **Deploy Independente**: Cada serviço pode escalar independentemente no Docker/Kubernetes
3. **Manutenção Facilitada**: Mudanças no Producer não afetam o Consumer
4. **Consistência**: Ambos usam as mesmas versões de Spring Boot e RabbitMQ configuradas no pai

### Producer API (Porta 8080)

- Expõe endpoint REST para criar registros de CDB
- Envia mensagens para o RabbitMQ
- Endpoint: `POST /api/v1/cdb-registry`

### Consumer API (Porta 8081)

- Consome mensagens do RabbitMQ
- Processa registros de CDB recebidos
- Logs de processamento

## Fluxo de Mensageria

```
Producer API → Exchange (fixed-income.direct)
            → Routing Key (cdb.registry.created)
            → Queue (fixed-income.cdb.registry)
            → Consumer API
```

## Pré-requisitos

- Docker e Docker Compose
- Java 21 (para desenvolvimento local)
- Maven 3.9+ (para desenvolvimento local)

## Como Executar

### Com Docker Compose (Recomendado)

```bash
# Build e start todos os serviços
docker-compose up --build

# Ou em background
docker-compose up -d --build

# Ver logs
docker-compose logs -f

# Parar os serviços
docker-compose down
```

### Desenvolvimento Local

```bash
# Build do projeto
mvn clean install

# Terminal 1 - RabbitMQ
docker-compose up rabbitmq

# Terminal 2 - Producer API
cd producer-api
mvn spring-boot:run

# Terminal 3 - Consumer API
cd consumer-api
mvn spring-boot:run
```

## Testando

### Criar um registro de CDB

```bash
curl -X POST http://localhost:8080/api/v1/cdb-registry \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "CLI-001",
    "amount": 10000.00,
    "durationDays": 365,
    "interestRate": 12.5
  }'
```

**Resposta esperada:**
```json
{
  "registryId": "fbe4bde1-53e1-4abd-aa1e-d4c1f6103eb7",
  "clientId": "CLI-001",
  "amount": 10000.00,
  "durationDays": 365,
  "interestRate": 12.5,
  "createdAt": "2025-10-23T17:35:56.121130754"
}
```

### Verificar Consumer

Verifique os logs do consumer para ver a mensagem sendo processada:

```bash
docker-compose logs -f consumer-api
```

Você deve ver logs como:
```
consumer-api  | Processing CDB registry: CdbRegistryDto(registryId=..., clientId=CLI-001, amount=10000.00, ...)
consumer-api  | Registry ID: ..., Client: CLI-001, Amount: 10000.00, Duration: 365 days, Interest Rate: 12.5%
```

## Acessos

- **Producer API**: http://localhost:8080
- **Consumer API**: http://localhost:8081
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)

## Estrutura dos Módulos

### Producer API
```
producer-api/src/main/java/
└── br/com/iagoomes/registrycdb/producer/
    ├── ProducerApplication.java
    ├── application/
    │   ├── controller/
    │   └── service/
    ├── domain/
    │   └── dto/
    └── infra/
        ├── config/
        └── mqprovider/producer/
```

### Consumer API
```
consumer-api/src/main/java/
└── br/com/iagoomes/registrycdb/consumer/
    ├── ConsumerApplication.java
    ├── domain/
    │   └── dto/
    └── infra/
        ├── config/
        └── mqprovider/consumer/
```

## Tecnologias

- Spring Boot 3.5.6
- Java 21
- RabbitMQ 3
- Maven Multi-Module
- Docker & Docker Compose
- Lombok
- Jackson
