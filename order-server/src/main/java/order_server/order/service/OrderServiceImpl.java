package order_server.order.service;

import br.com.exemplo.grpc.GetOrderStatusResponse;
import br.com.exemplo.grpc.OrderRequest;
import br.com.exemplo.grpc.OrderResponse;
import br.com.exemplo.grpc.OrderServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_server.order.dto.SimpleOrderDto;
import order_server.order.repository.OrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

  private final RabbitTemplate rabbitTemplate;
  private final OrderRepository orderRepository;

  @Value("${order.queue}")
  private String queue;

  @Override
  public void placeOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
    String orderId =
        request.getOrderId().isEmpty() ? UUID.randomUUID().toString() : request.getOrderId();
    log.info(
        "Recebido pedido para cliente: {}. ID do Pedido Gerado: {}",
        request.getCustomerName(),
        orderId);
    try {

      SimpleOrderDto orderDto =
          new SimpleOrderDto(
              orderId,
              request.getCustomerName(),
              request.getItemsList().stream()
                  .map(
                      item ->
                          new SimpleOrderDto.SimpleOrderItem(
                              item.getProductId(), item.getQuantity(), item.getPrice()))
                  .collect(Collectors.toList()),
              "PENDING_QUEUE");

      rabbitTemplate.convertAndSend(queue, orderDto);
      log.info("Pedido {} enviado com sucesso para a fila {}", orderId, queue);

      OrderResponse response =
          OrderResponse.newBuilder()
              .setOrderId(orderId)
              .setStatus("RECEIVED")
              .setMessage("Pedido recebido e sendo processado.")
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      log.error("Erro ao enviar pedido {} para a fila: {}", orderId, e.getMessage());
      responseObserver.onError(
          io.grpc.Status.INTERNAL
              .withDescription("Não foi possivel colocar o pedido na fila de processamento.")
              .asRuntimeException());
    }
  }

  @Override
  public void getOrderStatus(
      OrderRequest request, StreamObserver<GetOrderStatusResponse> responseObserver) {
    String orderId = request.getOrderId();
    log.info("Consultando status para o pedido: {}", orderId);

    try {
      orderRepository
          .findById(orderId)
          .map(
              orderDoc -> {
                GetOrderStatusResponse.Builder responseBuilder =
                    GetOrderStatusResponse.newBuilder()
                        .setOrderId(orderDoc.getId())
                        .setStatus(orderDoc.getStatus());

                if (orderDoc.getItems() != null) {
                  orderDoc
                      .getItems()
                      .forEach(
                          item ->
                              responseBuilder.addItems(
                                  br.com.exemplo.grpc.OrderItem.newBuilder()
                                      .setProductId(item.getProductId())
                                      .setQuantity(item.getQuantity())
                                      .setPrice(item.getPrice())
                                      .build()));
                }
                return responseBuilder.build();
              })
          .ifPresentOrElse(
              response -> {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
              },
              () -> {
                log.warn("Pedido {} não encontrado no banco de dados.", orderId);
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Pedido com ID " + orderId + " não encontrado.")
                        .asRuntimeException());
              });

    } catch (Exception e) {
      log.error("Erro ao consultar status do pedido {}: {}", orderId, e.getMessage(), e);
      responseObserver.onError(
          io.grpc.Status.INTERNAL
              .withDescription("Erro interno ao consultar o status do pedido.")
              .asRuntimeException());
    }
  }
}
