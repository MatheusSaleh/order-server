package order_server.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.exemplo.grpc.OrderItem;
import br.com.exemplo.grpc.OrderRequest;
import java.time.Instant;
import order_server.order.dto.OrderQueueMessage;
import order_server.order.model.Order;
import org.junit.jupiter.api.Test;

class OrderMapperTest {

  private static final OrderRequest REQUEST =
      OrderRequest.newBuilder()
          .setCustomerName("Ana")
          .addItems(
              OrderItem.newBuilder()
                  .setProductId("p1")
                  .setProductName("Teclado")
                  .setQuantity(2)
                  .setPrice(100.0))
          .addItems(
              OrderItem.newBuilder()
                  .setProductId("p2")
                  .setProductName("Mouse")
                  .setQuantity(1)
                  .setPrice(50.0))
          .build();

  @Test
  void calculaTotalQuandoNaoInformado() {
    assertThat(OrderMapper.resolveTotal(REQUEST)).isEqualTo(250.0);
  }

  @Test
  void respeitaTotalInformado() {
    OrderRequest withTotal = REQUEST.toBuilder().setTotalAmount(199.9).build();
    assertThat(OrderMapper.resolveTotal(withTotal)).isEqualTo(199.9);
  }

  @Test
  void converteRequestParaDocumentoEMensagem() {
    Instant now = Instant.parse("2026-01-01T10:00:00Z");
    Order order = OrderMapper.toDocument("abc", REQUEST, now);

    assertThat(order.getId()).isEqualTo("abc");
    assertThat(order.getItems()).hasSize(2);
    assertThat(order.getCreatedAt()).isEqualTo(now);

    OrderQueueMessage message = OrderMapper.toQueueMessage(order);
    assertThat(message.orderId()).isEqualTo("abc");
    assertThat(message.items()).extracting(OrderQueueMessage.Item::productName)
        .containsExactly("Teclado", "Mouse");
    assertThat(message.totalAmount()).isEqualTo(250.0);
  }

  @Test
  void converteDocumentoParaRespostaDeStatus() {
    Order order = OrderMapper.toDocument("abc", REQUEST, Instant.now());
    order.setStatus("COMPLETED");

    var response = OrderMapper.toStatusResponse(order);

    assertThat(response.getStatus()).isEqualTo("COMPLETED");
    assertThat(response.getCustomerName()).isEqualTo("Ana");
    assertThat(response.getItemsCount()).isEqualTo(2);
    assertThat(response.getLastUpdated()).isNotBlank();
  }
}
