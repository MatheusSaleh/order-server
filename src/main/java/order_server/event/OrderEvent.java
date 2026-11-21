package order_server.event;

import java.util.Map;

/**
 * Evento de ciclo de vida de um pedido. Publicado no exchange {@code order.events} e repassado ao
 * front-end via SSE pelo order-client.
 */
public record OrderEvent(
    String orderId,
    String stage,
    String service,
    long timestamp,
    String message,
    Map<String, Object> data) {}
