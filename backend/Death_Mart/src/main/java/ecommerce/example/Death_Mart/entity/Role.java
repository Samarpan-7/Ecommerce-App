package ecommerce.example.Death_Mart.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Role entity — maps to the "roles" table.
 * Each role has a unique RoleName (ROLE_CUSTOMER or ROLE_ADMIN).
 * Users are linked to roles via a many-to-many join table "user_roles".
 */
@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleName name;
}
