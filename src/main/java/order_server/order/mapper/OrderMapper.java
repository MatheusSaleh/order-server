package order_server.order.mapper;

import br.com.exemplo.grpc.GetOrderStatusResponse;
import br.com.exemplo.grpc.OrderRequest;
import java.time.Instant;
import java.util.List;
import order_server.order.dto.OrderQueueMessage;
import order_server.order.model.Order;
import order_server.order_item.model.OrderItem;

/** Conversoes entre Protobuf, documento MongoDB e mensagem da fila. */
public final class OrderMapper {

  private OrderMapper() {}

  public static Order toDocument(String orderId, OrderRequest request, Instant now) {
    List<OrderItem> items =
        request.getItemsList().stream()
            .map(
                item -> {
                  OrderItem doc = new OrderItem();
                  doc.setProductId(item.getProductId());
                  doc.setProductName(item.getProductName());
                  doc.setQuantity(item.getQuantity());
                  doc.setPrice(item.getPrice());
                  return doc;
                })
            .toList();

    Order order = new Order();
    order.setId(orderId);
    order.setCustomerName(request.getCustomerName());
    order.setItems(items);
    order.setTotalAmount(resolveTotal(request));
    order.setTimestamp(request.getTimestamp());
    order.setCreatedAt(now);
    order.setUpdatedAt(now);
    return order;
  }

  /** Usa o total informado; se vier zerado, calcula a partir dos itens. */
  public static double resolveTotal(OrderRequest request) {
    if (request.getTotalAmount() > 0) {
      return request.getTotalAmount();
    }
    return request.getItemsList().stream()
        .mapToDouble(item -> item.getPrice() * item.getQuantity())
        .sum();
  }

  public static OrderQueueMessage toQueueMessage(Order order) {
    return new OrderQueueMessage(
        order.getId(),
        order.getCustomerName(),
        order.getItems().stream()
            .map(
                item ->
                    new OrderQueueMessage.Item(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()))
            .toList(),
        order.getTotalAmount(),
        order.getCreatedAt());
  }

  public static GetOrderStatusResponse toStatusResponse(Order order) {
    GetOrderStatusResponse.Builder builder =
        GetOrderStatusResponse.newBuilder()
            .setOrderId(order.getId())
            .setStatus(nullToEmpty(order.getStatus()))
            .setCustomerName(nullToEmpty(order.getCustomerName()))
            .setTotalAmount(order.getTotalAmount());

    if (order.getUpdatedAt() != null) {
      builder.setLastUpdated(order.getUpdatedAt().toString());
    }
    if (order.getItems() != null) {
      order
          .getItems()
          .forEach(
              item ->
                  builder.addItems(
                      br.com.exemplo.grpc.OrderItem.newBuilder()
                          .setProductId(nullToEmpty(item.getProductId()))
                          .setProductName(nullToEmpty(item.getProductName()))
                          .setQuantity(item.getQuantity())
                          .setPrice(item.getPrice())
                          .build()));
    }
    return builder.build();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
