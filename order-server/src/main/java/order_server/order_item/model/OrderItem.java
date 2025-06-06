package order_server.order_item.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
    private String productId;

    private String productName;

    private int quantity;

    private double price;
}
