# order-server

Servidor **gRPC** da plataforma de pedidos. Visão geral e como rodar tudo: **[order-platform](https://github.com/MatheusSaleh/order-platform)**.

![Plataforma em funcionamento](https://raw.githubusercontent.com/MatheusSaleh/order-platform/main/docs/demo.gif)

<sub>O order-server é o nó "order-server" do diagrama: recebe o gRPC, grava no MongoDB e publica na fila.</sub>

## Responsabilidades

- Implementa `OrderService` do [grpc-commons](https://github.com/MatheusSaleh/grpc-commons)
- Valida toda entrada (tamanhos, intervalos e caracteres), sem confiar em quem chama
- Grava com `insert`: um `orderId` repetido retorna `ALREADY_EXISTS` e nunca sobrescreve outro pedido
- `PlaceOrder`: valida, grava o pedido no MongoDB como `PENDING` e publica em `pedidos_fila` (JSON)
- `GetOrderStatus`: lê o documento no MongoDB
- Declara a topologia do RabbitMQ (fila com dead-letter para `pedidos_fila.dlq`)
- Publica eventos `SERVER_RECEIVED`, `SAVED_PENDING` e `QUEUED` no exchange `order.events`

| Porta | Uso |
|---|---|
| 9090 | gRPC (reflection desligado por padrão; `GRPC_REFLECTION_ENABLED=true` habilita o `grpcurl`) |
| 8081 | HTTP / Actuator (`/actuator/health`) |

## Rodando localmente

Requer RabbitMQ e MongoDB (o `docker compose` do order-platform sobe os dois) e o `grpc-commons` instalado no `~/.m2` ou publicado no JitPack.

```bash
./mvnw spring-boot:run
```

Variáveis: `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `MONGODB_URI`, `GRPC_PORT`, `GRPC_REFLECTION_ENABLED`, `ORDER_QUEUE`.

Stack: Java 21, Spring Boot 4.2 (`spring-boot-starter-grpc-server`), Spring AMQP 4, Spring Data MongoDB.
