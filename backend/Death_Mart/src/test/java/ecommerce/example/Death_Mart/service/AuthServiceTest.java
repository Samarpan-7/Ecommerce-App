package ecommerce.example.Death_Mart.service;

import ecommerce.example.Death_Mart.dto.request.RegisterRequest;
import ecommerce.example.Death_Mart.dto.response.UserResponse;
import ecommerce.example.Death_Mart.entity.Role;
import ecommerce.example.Death_Mart.entity.RoleName;
import ecommerce.example.Death_Mart.entity.User;
import ecommerce.example.Death_Mart.exception.BadRequestException;
import ecommerce.example.Death_Mart.repository.CartRepository;
import ecommerce.example.Death_Mart.repository.RoleRepository;
import ecommerce.example.Death_Mart.repository.UserRepository;
import ecommerce.example.Death_Mart.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private CartRepository cartRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        cartRepository = mock(CartRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userRepository,
                roleRepository,
                cartRepository,
                passwordEncoder,
                mock(AuthenticationManager.class),
                mock(JwtTokenProvider.class)
        );
    }

    @Test
    void registrationStoresHashedPasswordAndNormalizesEmail() {
        Role customerRole = Role.builder().name(RoleName.ROLE_CUSTOMER).build();
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(17L);
            return user;
        });

        UserResponse response = authService.register(RegisterRequest.builder()
                .fullName("Test Customer")
                .email("  CUSTOMER@example.com ")
                .password("correct-horse")
                .confirmPassword("correct-horse")
                .build());

        verify(userRepository).existsByEmailIgnoreCase("customer@example.com");
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("customer@example.com")
                        && passwordEncoder.matches("correct-horse", user.getPassword())));
        verify(cartRepository).save(any());
        assertEquals(17L, response.getId());
        assertEquals("customer@example.com", response.getEmail());
    }

    @Test
    void registrationRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(RegisterRequest.builder()
                .fullName("Test Customer")
                .email("CUSTOMER@example.com")
                .password("correct-horse")
                .confirmPassword("correct-horse")
                .build()));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registrationRejectsMismatchedPasswords() {
        assertThrows(BadRequestException.class, () -> authService.register(RegisterRequest.builder()
                .fullName("Test Customer")
                .email("customer@example.com")
                .password("correct-horse")
                .confirmPassword("different-password")
                .build()));

        verifyNoInteractions(userRepository, roleRepository, cartRepository);
    }
}
