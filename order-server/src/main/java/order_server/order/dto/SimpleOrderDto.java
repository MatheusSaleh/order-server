package order_server.order.dto;

import java.util.List;

public record SimpleOrderDto(
    String orderId, String customerId, List<SimpleOrderItem> items, String initialStatus) {
  public record SimpleOrderItem(String productId, int quantity, double price) {}
}
