package ecommerce.example.Death_Mart.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * CartItem entity — maps to the "cart_items" table.
 *
 * Represents a single product entry in a user's cart with a quantity.
 * If the same product is added again, the quantity is incremented
 * rather than creating a duplicate entry.
 */
@Entity
@Table(name = "cart_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;
}
