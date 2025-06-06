package order_server.order.service;

import br.com.exemplo.grpc.OrderRequest;
import br.com.exemplo.grpc.OrderResponse;
import br.com.exemplo.grpc.OrderServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import order_server.order.model.Order;
import order_server.order.repository.OrderRepository;
import order_server.order_item.model.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderRepository orderRepository;

    @Value("${order.queue}")
    private String queue;

    @Override
    public void placeOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        try {
            String orderId = request.getOrderId().isEmpty() ? UUID.randomUUID().toString() : request.getOrderId();

            List<OrderItem> itemList = request.getItemsList().stream().map(item -> {
                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(item.getProductId());
                orderItem.setProductName(item.getProductName());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPrice(item.getPrice());
                return orderItem;
            }).toList();

            Order order = new Order();
            order.setId(orderId);
            order.setCustomerName(request.getCustomerName());
            order.setItems(itemList);
            order.setTotalAmount(request.getTotalAmount());
            order.setTimestamp(request.getTimestamp());
            order.setStatus("RECEIVED");

            orderRepository.save(order);

            OrderRequest updatedRequest = OrderRequest.newBuilder(request)
                    .setOrderId(orderId)
                    .build();
            rabbitTemplate.convertAndSend(queue, objectMapper.writeValueAsString(updatedRequest));

            OrderResponse response = OrderResponse.newBuilder()
                    .setOrderId(orderId)
                    .setStatus("RECEIVED")
                    .setMessage("Pedido recebido e salvo com sucesso.")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }


}
