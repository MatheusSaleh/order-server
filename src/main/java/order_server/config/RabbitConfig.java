package order_server.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Topologia do RabbitMQ usada pelo order-server.
 *
 * <ul>
 *   <li><b>pedidos_fila</b>: fila de trabalho consumida pelo order-worker. Mensagens rejeitadas vao
 *       para a DLQ.
 *   <li><b>pedidos_fila.dlq</b>: dead letter queue com pedidos que falharam apos as tentativas.
 *   <li><b>order.events</b>: exchange topic com os eventos de ciclo de vida de cada pedido,
 *       consumidos pelo order-client para alimentar o front-end em tempo real.
 * </ul>
 *
 * <p>A declaracao das filas e identica no order-worker: quem subir primeiro cria a topologia.
 */
@Configuration
public class RabbitConfig {

  public static final String EVENTS_EXCHANGE = "order.events";

  @Bean
  MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
    return new JacksonJsonMessageConverter(jsonMapper);
  }

  @Bean
  Queue ordersQueue(@Value("${order.queue}") String queue) {
    return QueueBuilder.durable(queue)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", queue + ".dlq")
        .build();
  }

  @Bean
  Queue ordersDeadLetterQueue(@Value("${order.queue}") String queue) {
    return QueueBuilder.durable(queue + ".dlq").build();
  }

  @Bean
  TopicExchange orderEventsExchange() {
    return new TopicExchange(EVENTS_EXCHANGE, true, false);
  }
}
