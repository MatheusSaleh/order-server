package order_server.order.service;

import br.com.exemplo.grpc.OrderItem;
import br.com.exemplo.grpc.OrderRequest;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validacao de entrada do lado do servidor. O order-client ja valida, mas o servidor gRPC nao
 * confia em quem chama (defesa em profundidade): limita tamanhos para evitar documentos gigantes
 * no MongoDB e restringe caracteres para evitar injecao em logs.
 */
public final class OrderRequestValidator {

  public static final int MAX_ITEMS = 20;
  public static final int MAX_QUANTITY = 99;
  public static final double MAX_PRICE = 100_000;

  private static final Pattern CUSTOMER_NAME = Pattern.compile("^[\\p{L}\\p{N} .'-]{1,80}$");
  private static final Pattern PRODUCT_ID = Pattern.compile("^[A-Za-z0-9_-]{1,40}$");
  private static final Pattern PRODUCT_NAME = Pattern.compile("^[\\p{L}\\p{N} .,'\"()/+&-]{0,80}$");
  private static final Pattern UUID =
      Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private static final Pattern TIMESTAMP = Pattern.compile("^[0-9A-Za-z:.+-]{0,40}$");

  private OrderRequestValidator() {}

  /** Retorna a mensagem de erro, ou vazio se o pedido for valido. */
  public static Optional<String> validate(OrderRequest request) {
    if (!request.getOrderId().isEmpty() && !UUID.matcher(request.getOrderId()).matches()) {
      return Optional.of("orderId deve ser um UUID.");
    }
    if (!CUSTOMER_NAME.matcher(request.getCustomerName()).matches()) {
      return Optional.of("customerName invalido (1 a 80 letras, numeros ou espacos).");
    }
    if (request.getItemsCount() == 0 || request.getItemsCount() > MAX_ITEMS) {
      return Optional.of("O pedido deve ter entre 1 e " + MAX_ITEMS + " itens.");
    }
    if (!TIMESTAMP.matcher(request.getTimestamp()).matches()) {
      return Optional.of("timestamp invalido.");
    }
    if (request.getTotalAmount() < 0 || request.getTotalAmount() > MAX_PRICE * MAX_QUANTITY * MAX_ITEMS) {
      return Optional.of("totalAmount fora do intervalo permitido.");
    }
    for (OrderItem item : request.getItemsList()) {
      if (!PRODUCT_ID.matcher(item.getProductId()).matches()) {
        return Optional.of("productId invalido.");
      }
      if (!PRODUCT_NAME.matcher(item.getProductName()).matches()) {
        return Optional.of("productName invalido.");
      }
      if (item.getQuantity() < 1 || item.getQuantity() > MAX_QUANTITY) {
        return Optional.of("quantity deve estar entre 1 e " + MAX_QUANTITY + ".");
      }
      if (!(item.getPrice() >= 0 && item.getPrice() <= MAX_PRICE)) {
        return Optional.of("price fora do intervalo permitido.");
      }
    }
    return Optional.empty();
  }
}
