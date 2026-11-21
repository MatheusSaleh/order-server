package order_server.order.service;

import br.com.exemplo.grpc.GetOrderStatusRequest;
import br.com.exemplo.grpc.GetOrderStatusResponse;
import br.com.exemplo.grpc.OrderRequest;
import br.com.exemplo.grpc.OrderResponse;
import br.com.exemplo.grpc.OrderServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_server.event.OrderEventPublisher;
import order_server.order.mapper.OrderMapper;
import order_server.order.model.Order;
import order_server.order.repository.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

  private final RabbitTemplate rabbitTemplate;
  private final OrderRepository orderRepository;
  private final OrderEventPublisher events;

  @Value("${order.queue}")
  private String queue;

  @Override
  public void placeOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
    var validationError = OrderRequestValidator.validate(request);
    if (validationError.isPresent()) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(validationError.get()).asRuntimeException());
      return;
    }

    String orderId =
        request.getOrderId().isBlank() ? UUID.randomUUID().toString() : request.getOrderId();
    log.info("PlaceOrder recebido: pedido={} cliente={}", orderId, request.getCustomerName());

    events.publish(
        orderId,
        "SERVER_RECEIVED",
        "order-server recebeu a chamada gRPC PlaceOrder",
        Map.of(
            "rpc", "order.OrderService/PlaceOrder",
            "protobufBytes", request.getSerializedSize(),
            "items", request.getItemsCount()));

    try {
      Order order = OrderMapper.toDocument(orderId, request, Instant.now());
      order.setStatus("PENDING");
      // insert (e nao save): um orderId repetido nunca sobrescreve um pedido existente
      orderRepository.insert(order);
      events.publish(
          orderId,
          "SAVED_PENDING",
          "Pedido gravado no MongoDB com status PENDING",
          Map.of("collection", "orders", "status", "PENDING", "totalAmount", order.getTotalAmount()));

      rabbitTemplate.convertAndSend(queue, OrderMapper.toQueueMessage(order));
      events.publish(
          orderId,
          "QUEUED",
          "Pedido publicado na fila " + queue,
          Map.of("queue", queue, "format", "application/json"));

      responseObserver.onNext(
          OrderResponse.newBuilder()
              .setOrderId(orderId)
              .setStatus("RECEIVED")
              .setMessage("Pedido recebido e enviado para processamento.")
              .build());
      responseObserver.onCompleted();

    } catch (DuplicateKeyException e) {
      responseObserver.onError(
          Status.ALREADY_EXISTS
              .withDescription("Ja existe um pedido com este ID.")
              .asRuntimeException());
    } catch (Exception e) {
      log.error("Erro ao registrar o pedido {}: {}", orderId, e.getMessage(), e);
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("Nao foi possivel registrar o pedido para processamento.")
              .asRuntimeException());
    }
  }

  @Override
  public void getOrderStatus(
      GetOrderStatusRequest request, StreamObserver<GetOrderStatusResponse> responseObserver) {
    String orderId = request.getOrderId();
    if (orderId.isEmpty() || orderId.length() > 64) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription("order_id invalido.").asRuntimeException());
      return;
    }
    log.info("GetOrderStatus: pedido={}", orderId);

    try {
      orderRepository
          .findById(orderId)
          .map(OrderMapper::toStatusResponse)
          .ifPresentOrElse(
              response -> {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
              },
              () ->
                  responseObserver.onError(
                      Status.NOT_FOUND
                          .withDescription("Pedido com ID " + orderId + " nao encontrado.")
                          .asRuntimeException()));
    } catch (Exception e) {
      log.error("Erro ao consultar o pedido {}: {}", orderId, e.getMessage(), e);
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("Erro interno ao consultar o status do pedido.")
              .asRuntimeException());
    }
  }
}
