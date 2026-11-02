package ecommerce.example.Death_Mart.dto.response;

import ecommerce.example.Death_Mart.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private AddressResponse shippingAddress;
    private BigDecimal totalAmount;
    private OrderStatus status;
    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
    private PaymentResponse payment;
    private LocalDateTime orderDate;
}
