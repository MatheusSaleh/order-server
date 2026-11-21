package order_server.event;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order_server.config.RabbitConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de observabilidade. E "best effort": uma falha aqui nunca deve impedir o
 * processamento do pedido em si.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

  private static final String SERVICE = "order-server";

  private final RabbitTemplate rabbitTemplate;

  public void publish(String orderId, String stage, String message, Map<String, Object> data) {
    OrderEvent event =
        new OrderEvent(orderId, stage, SERVICE, System.currentTimeMillis(), message, data);
    try {
      rabbitTemplate.convertAndSend(
          RabbitConfig.EVENTS_EXCHANGE, "order." + stage.toLowerCase(), event);
    } catch (AmqpException e) {
      log.warn("Nao foi possivel publicar o evento {} do pedido {}: {}", stage, orderId, e.getMessage());
    }
  }
}
