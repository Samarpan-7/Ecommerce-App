package ecommerce.example.Death_Mart.controller;

import ecommerce.example.Death_Mart.dto.request.UpdateOrderStatusRequest;
import ecommerce.example.Death_Mart.dto.response.*;
import ecommerce.example.Death_Mart.entity.RoleName;
import ecommerce.example.Death_Mart.service.ContactService;
import ecommerce.example.Death_Mart.service.DashboardService;
import ecommerce.example.Death_Mart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final OrderService orderService;
    private final ContactService contactService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        PagedResponse<OrderResponse> orders = orderService.getAllOrdersAdmin(page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        PagedResponse<UserResponse> users = dashboardService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String roleStr = payload.get("role");
        RoleName roleName = RoleName.valueOf(roleStr);
        dashboardService.updateUserRole(id, roleName);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", null));
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<ContactMessageResponse>>> getAllMessages() {
        List<ContactMessageResponse> messages = contactService.getAllMessages();
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PutMapping("/messages/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markMessageAsRead(@PathVariable Long id) {
        contactService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Message marked as read", null));
    }
}
