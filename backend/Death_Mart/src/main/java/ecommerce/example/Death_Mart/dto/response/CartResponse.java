package ecommerce.example.Death_Mart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long id;
    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();
    private Integer totalQuantity;
    private BigDecimal totalAmount;
}
