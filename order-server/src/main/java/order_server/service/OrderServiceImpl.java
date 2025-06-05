package order_server.service;

import br.com.exemplo.grpc.OrderRequest;
import br.com.exemplo.grpc.OrderResponse;
import br.com.exemplo.grpc.OrderServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${order.queue}")
    private String queue;

    @Override
    public void placeOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        try {
            String orderId = request.getOrderId().isEmpty() ? UUID.randomUUID().toString() : request.getOrderId();

            OrderRequest updatedRequest = OrderRequest.newBuilder(request)
                    .setOrderId(orderId)
                    .build();

            rabbitTemplate.convertAndSend(queue, objectMapper.writeValueAsString(updatedRequest));

            OrderResponse response = OrderResponse.newBuilder()
                    .setOrderId(orderId)
                    .setStatus("RECEIVED")
                    .setMessage("Pedido recebido com sucesso")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }


}
