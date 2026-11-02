package ecommerce.example.Death_Mart.service;

import ecommerce.example.Death_Mart.dto.response.DashboardStatsResponse;
import ecommerce.example.Death_Mart.dto.response.PagedResponse;
import ecommerce.example.Death_Mart.dto.response.UserResponse;
import ecommerce.example.Death_Mart.entity.OrderStatus;
import ecommerce.example.Death_Mart.entity.Role;
import ecommerce.example.Death_Mart.entity.RoleName;
import ecommerce.example.Death_Mart.entity.User;
import ecommerce.example.Death_Mart.exception.ResourceNotFoundException;
import ecommerce.example.Death_Mart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING) +
                orderRepository.countByStatus(OrderStatus.PROCESSING);
        long lowStock = productRepository.countByStockQuantityLessThan(10);
        long unreadMessages = contactMessageRepository.countByIsReadFalse();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .pendingOrders(pendingOrders)
                .lowStockProducts(lowStock)
                .unreadMessages(unreadMessages)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserResponse> content = userPage.getContent().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PagedResponse.<UserResponse>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Transactional
    public void updateUserRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));

        user.setRoles(Collections.singleton(role));
        userRepository.save(user);
    }
}
