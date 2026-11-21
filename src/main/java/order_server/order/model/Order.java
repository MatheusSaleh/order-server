package order_server.order.model;

import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import order_server.order_item.model.OrderItem;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "orders")
public class Order {

  @Id private String id;

  private String customerName;

  private List<OrderItem> items;

  private double totalAmount;

  /** Timestamp informado pelo cliente (texto livre). */
  private String timestamp;

  /** PENDING, PROCESSING, COMPLETED ou FAILED. */
  private String status;

  private Instant createdAt;

  private Instant updatedAt;
}
