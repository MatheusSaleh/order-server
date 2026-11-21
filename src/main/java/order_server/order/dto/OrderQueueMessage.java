package order_server.order.dto;

import java.time.Instant;
import java.util.List;

/** Mensagem publicada na fila de trabalho (serializada como JSON). */
public record OrderQueueMessage(
    String orderId,
    String customerName,
    List<Item> items,
    double totalAmount,
    Instant createdAt) {

  public record Item(String productId, String productName, int quantity, double price) {}
}
