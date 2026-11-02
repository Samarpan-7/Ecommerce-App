package ecommerce.example.Death_Mart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

/**
 * Category entity — maps to the "categories" table.
 *
 * Products are organized into categories (e.g. Books, Electronics, Food).
 * One category can contain many products (one-to-many).
 */
@Entity
@Table(name = "categories")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
