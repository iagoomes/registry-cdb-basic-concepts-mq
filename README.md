# Registry CDB Basic Concepts MQ

Este projeto demonstra o **fluxo básico de mensageria** com RabbitMQ em um contexto de renda fixa (CDB).

## 📋 Sobre o Projeto

Este é o **Exercício 1** da série de aprendizado de RabbitMQ, focado no fluxo fundamental Producer → Exchange → Queue → Consumer.

**O que você aprenderá:**
- ✅ Fluxo básico de mensageria
- ✅ Producer API (enviar mensagens)
- ✅ Consumer API (receber mensagens)
- ✅ Direct Exchange (roteamento exato)
- ✅ Estrutura Maven Multi-Module
- ✅ Event-driven architecture

## 🏗️ Arquitetura

```
Producer API (8080) → RabbitMQ Direct Exchange → Consumer API (8081)
                           ↓
                    fixed-income.direct
                           │
                    Routing Key: cdb.registry.created
                           │
                    fixed-income.cdb.registry (Queue)
                           │
                           ↓
                    Consumer API processa
```

## 📦 Estrutura do Projeto

O projeto utiliza **Maven Multi-Module**, separando Producer e Consumer em módulos independentes:

```
registry-cdb-basic-concepts-mq/
├── pom.xml (POM Pai)
├── producer-api/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/
└── consumer-api/
    ├── pom.xml
    ├── Dockerfile
    └── src/main/java/
```

**Vantagens:**
- Deploy independente de Producer e Consumer
- Escalabilidade horizontal isolada
- Cada módulo com suas próprias dependências
- Simula arquitetura de microserviços

## 🛠️ Tecnologias

- Java 21
- Spring Boot 3.5.6
- Spring AMQP
- RabbitMQ 3
- Docker & Docker Compose
- Maven Multi-Module
- Lombok
- Jackson

## ✅ Pré-requisitos

- Docker e Docker Compose instalados
- Java 21 (para desenvolvimento local)
- Maven 3.9+ (para desenvolvimento local)
- Portas 8080, 8081, 5672 e 15672 disponíveis

## 🚀 Como Executar

### Com Docker Compose (recomendado)

```bash
# Clonar o repositório
git clone https://github.com/iagoomes/registry-cdb-basic-concepts-mq.git
cd registry-cdb-basic-concepts-mq

# Buildar e iniciar todos os serviços
docker-compose up --build

# Ou em background
docker-compose up -d --build

# Ver logs
docker-compose logs -f

# Parar os serviços
docker-compose down
```

### Em Desenvolvimento Local

```bash
# Terminal 1 - RabbitMQ
docker-compose up rabbitmq

# Terminal 2 - Build do projeto
mvn clean install

# Terminal 3 - Producer API
cd producer-api
mvn spring-boot:run

# Terminal 4 - Consumer API
cd consumer-api
mvn spring-boot:run
```

## 📁 Estrutura de Código

### Producer API

```
producer-api/src/main/java/br/com/iagoomes/registrycdb/producer/
├── ProducerApplication.java
├── application/
│   ├── controller/
│   │   └── CdbRegistryController.java
│   └── service/
│       └── CdbRegistryService.java
├── domain/
│   └── dto/
│       └── CdbRegistryDto.java
└── infra/
    ├── config/
    │   └── RabbitMQConfig.java
    └── mqprovider/producer/
        └── CdbRegistryProducer.java
```

### Consumer API

```
consumer-api/src/main/java/br/com/iagoomes/registrycdb/consumer/
├── ConsumerApplication.java
├── domain/
│   └── dto/
│       └── CdbRegistryDto.java
└── infra/
    ├── config/
    │   └── RabbitMQConfig.java
    └── mqprovider/consumer/
        └── CdbRegistryConsumer.java
```

## 📡 Endpoints da API

### Producer API (http://localhost:8080)

**Health Check**
```
GET /api/v1/cdb-registry/health
```

**Criar Registro de CDB**
```bash
POST /api/v1/cdb-registry
Content-Type: application/json

{
  "clientId": "CLI-001",
  "amount": 10000.00,
  "durationDays": 365,
  "interestRate": 12.5
}
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

## 📚 Fluxo de Mensageria

### Como Funciona

O RabbitMQ utiliza um modelo **Producer-Exchange-Queue-Consumer**:

1. **Producer**: Envia mensagem para o Exchange
2. **Exchange**: Roteia a mensagem para a Queue baseado na Routing Key
3. **Queue**: Armazena a mensagem até o Consumer processar
4. **Consumer**: Recebe e processa a mensagem

### Direct Exchange

Este projeto usa **Direct Exchange**, que roteia mensagens com base em **correspondência exata** da routing key.

**Exemplo:**
- Producer envia com routing key: `cdb.registry.created`
- Queue está vinculada com pattern: `cdb.registry.created` (exato)
- ✅ Mensagem é entregue à queue

Se a routing key não corresponder exatamente:
- Producer envia com routing key: `cdb.registry.updated`
- Queue está vinculada com pattern: `cdb.registry.created`
- ❌ Mensagem NÃO é entregue

### Fluxo Passo-a-Passo

```
1. POST /api/v1/cdb-registry (Producer recebe HTTP)
   ↓
2. CdbRegistryService.save() cria o DTO
   ↓
3. CdbRegistryProducer envia para Exchange
   - Exchange: fixed-income.direct
   - Routing Key: cdb.registry.created
   - Payload: CdbRegistryDto
   ↓
4. Exchange compara Routing Key com Bindings
   - Binding: fixed-income.cdb.registry ← cdb.registry.created
   - ✅ MATCH! Mensagem vai para a queue
   ↓
5. Mensagem fica na fila: fixed-income.cdb.registry
   ↓
6. Consumer escuta a fila com @RabbitListener
   - Consome a mensagem
   - Desserializa o JSON para CdbRegistryDto
   - Processa (log, negócio, etc)
   ↓
7. Consumer retorna ACK (acknowledgment)
   - Mensagem é confirmada e removida da fila
   ↓
8. ✅ Processamento concluído
```

## 💡 Exemplos de Uso

### Exemplo 1: Criar um CDB

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

**Logs esperados do consumer:**

```
consumer-api | Processing CDB registry: CdbRegistryDto(registryId=..., clientId=CLI-001, ...)
consumer-api | Registry ID: ..., Client: CLI-001, Amount: 10000.00, Duration: 365 days, Interest Rate: 12.5%
```

### Exemplo 2: Múltiplas Requisições

```bash
# Requisição 1
curl -X POST http://localhost:8080/api/v1/cdb-registry \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "CLI-002",
    "amount": 50000.00,
    "durationDays": 720,
    "interestRate": 13.0
  }'

# Requisição 2
curl -X POST http://localhost:8080/api/v1/cdb-registry \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "CLI-003",
    "amount": 25000.00,
    "durationDays": 540,
    "interestRate": 11.8
  }'
```

Consumer processa ambas sequencialmente:

```
consumer-api | Processing CDB registry: ..., clientId=CLI-002, ...
consumer-api | Registry ID: ..., Client: CLI-002, Amount: 50000.00, ...
consumer-api | Processing CDB registry: ..., clientId=CLI-003, ...
consumer-api | Registry ID: ..., Client: CLI-003, Amount: 25000.00, ...
```

## 📊 Monitoramento

### Ver logs do Consumer

```bash
docker-compose logs -f consumer-api
```

Você verá algo como:

```
consumer-api | 2025-10-24 15:30:00 INFO Processing CDB registry: ...
consumer-api | 2025-10-24 15:30:00 INFO Registry ID: ..., Client: CLI-001, ...
```

### RabbitMQ Management UI

Acesse: [http://localhost:15672](http://localhost:15672)

**Credenciais:**
- Username: `guest`
- Password: `guest`

**O que você pode ver:**

1. **Exchanges**
   - Vá em Exchanges → `fixed-income.direct`
   - Veja o binding com a fila

2. **Queues**
   - Vá em Queues
   - Veja a fila: `fixed-income.cdb.registry`
   - Clique em "Get Messages" para visualizar conteúdo
   - Veja o Ready count (mensagens aguardando)

3. **Connections**
   - Veja as conexões ativas do Producer e Consumer

## ⚙️ Configuração

### application.yml

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

fixed-income:
  queue:
    name: fixed-income.cdb.registry
    exchange: fixed-income.direct
    routing-key: cdb.registry.created
```

### Variáveis de Ambiente

Você pode sobrescrever as configurações via variáveis de ambiente:

```bash
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
```

## 🔧 Troubleshooting

### Verificar se as portas estão em uso

```bash
lsof -i :8080
lsof -i :8081
lsof -i :5672
lsof -i :15672
```

### Parar containers antigos

```bash
docker-compose down -v
```

### Verificar logs do Producer

```bash
docker-compose logs -f producer-api
```

Você deve ver logs de operação normais.

### Verificar conexão com RabbitMQ

- Acesse [http://localhost:15672](http://localhost:15672)
- Vá em Connections
- Deve haver uma conexão do Producer

### Consumer não está recebendo mensagens

**Verificar:**
- Consumer está rodando?
  ```bash
  docker-compose ps
  ```
- Queue foi criada?
  - Acesse [http://localhost:15672](http://localhost:15672) → Queues
  - Deve existir `fixed-income.cdb.registry`
- Binding está correto?
  - Acesse Exchange `fixed-income.direct`
  - Veja se o binding está configurado

**Possíveis causas:**
- Consumer travado (verificar logs)
- Erro na desserialização JSON
- Exception no handler do consumer

**Solução:**
```bash
# Reiniciar o consumer
docker-compose restart consumer-api

# Ver logs detalhados
docker-compose logs -f consumer-api
```

## 📚 Série de Exercícios

- **Exercício 1:** [registry-cdb-basic-concepts-mq](https://github.com/iagoomes/registry-cdb-basic-concepts-mq) - Fluxo básico ← VOCÊ ESTÁ AQUI
- **Exercício 2:** [registry-cdb-dlx-retry-mq](https://github.com/iagoomes/registry-cdb-dlx-retry-mq) - DLX e Retry
- **Exercício 3:** [fixed-income-topic-routing-mq](https://github.com/iagoomes/fixed-income-topic-routing-mq) - Topic Exchange

**Próximos:**
- Exercício 4: Fanout Exchange (Broadcasting)
- Exercício 5: Priority Queues
- Exercício 6: Delayed Messages com TTL
- Exercício 7: Idempotência e Deduplicação
- Exercício 8: Saga Pattern

## 👨‍💻 Autor

**Iago Gomes**
- GitHub: [@iagoomes](https://github.com/iagoomes)
- LinkedIn: [Iago Gomes](https://www.linkedin.com/in/deviagogomes)

⭐ Se este projeto te ajudou, deixe uma estrela no repositório!
