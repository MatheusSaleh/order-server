package order_server.order.model;

import lombok.Getter;
import lombok.Setter;
import order_server.order_item.model.OrderItem;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String customerName;

    private List<OrderItem> items;

    private double totalAmount;

    private String timestamp;

    private String status;
}
