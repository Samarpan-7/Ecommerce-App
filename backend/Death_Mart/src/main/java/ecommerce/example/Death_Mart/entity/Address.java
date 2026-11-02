package ecommerce.example.Death_Mart.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Address entity — maps to the "addresses" table.
 *
 * Each user can have multiple shipping addresses.
 * One address can be marked as the default for checkout convenience.
 */
@Entity
@Table(name = "addresses")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String country = "India";

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
}
