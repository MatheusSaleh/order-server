package order_server.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.exemplo.grpc.OrderItem;
import br.com.exemplo.grpc.OrderRequest;
import org.junit.jupiter.api.Test;

class OrderRequestValidatorTest {

  private static OrderRequest.Builder valid() {
    return OrderRequest.newBuilder()
        .setCustomerName("Ana Souza")
        .addItems(OrderItem.newBuilder().setProductId("kb-01").setProductName("Teclado").setQuantity(1).setPrice(10));
  }

  @Test
  void aceitaPedidoValido() {
    assertThat(OrderRequestValidator.validate(valid().build())).isEmpty();
  }

  @Test
  void rejeitaOrderIdQueNaoEUuid() {
    assertThat(OrderRequestValidator.validate(valid().setOrderId("../../admin").build())).isPresent();
  }

  @Test
  void rejeitaCaracteresDeControleNoNome() {
    assertThat(OrderRequestValidator.validate(valid().setCustomerName("Ana\r\nFAKE LOG").build())).isPresent();
    assertThat(OrderRequestValidator.validate(valid().setCustomerName("<script>").build())).isPresent();
  }

  @Test
  void rejeitaPedidosGrandesDemais() {
    OrderRequest.Builder builder = valid();
    for (int i = 0; i < OrderRequestValidator.MAX_ITEMS; i++) {
      builder.addItems(OrderItem.newBuilder().setProductId("p" + i).setQuantity(1).setPrice(1));
    }
    assertThat(OrderRequestValidator.validate(builder.build())).isPresent();
    assertThat(OrderRequestValidator.validate(valid().setCustomerName("a".repeat(81)).build())).isPresent();
  }

  @Test
  void rejeitaValoresForaDoIntervalo() {
    OrderRequest negative = OrderRequest.newBuilder().setCustomerName("Ana")
        .addItems(OrderItem.newBuilder().setProductId("p").setQuantity(1).setPrice(-5)).build();
    OrderRequest nan = OrderRequest.newBuilder().setCustomerName("Ana")
        .addItems(OrderItem.newBuilder().setProductId("p").setQuantity(1).setPrice(Double.NaN)).build();
    OrderRequest zeroQty = OrderRequest.newBuilder().setCustomerName("Ana")
        .addItems(OrderItem.newBuilder().setProductId("p").setQuantity(0).setPrice(1)).build();
    assertThat(OrderRequestValidator.validate(negative)).isPresent();
    assertThat(OrderRequestValidator.validate(nan)).isPresent();
    assertThat(OrderRequestValidator.validate(zeroQty)).isPresent();
  }
}
