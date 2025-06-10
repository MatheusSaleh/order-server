package order_server.order.dto;

import java.io.Serializable;
import java.util.List;

public record SimpleOrderDto(
        String orderId,
        String customerId,
        List<SimpleOrderItem> items,
        String initialStatus
) implements Serializable {

  public record SimpleOrderItem(
          String productId,
          int quantity,
          double price
  ) implements Serializable {}
}
