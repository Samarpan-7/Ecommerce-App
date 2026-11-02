package ecommerce.example.Death_Mart.controller;

import ecommerce.example.Death_Mart.dto.request.CartItemRequest;
import ecommerce.example.Death_Mart.dto.response.ApiResponse;
import ecommerce.example.Death_Mart.dto.response.CartResponse;
import ecommerce.example.Death_Mart.exception.BadRequestException;
import ecommerce.example.Death_Mart.security.CurrentUser;
import ecommerce.example.Death_Mart.security.UserPrincipal;
import ecommerce.example.Death_Mart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@CurrentUser UserPrincipal currentUser) {
        CartResponse cart = cartService.getCart(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse cart = cartService.addToCart(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> payload) {
        Integer quantity = payload.get("quantity");
        if (quantity == null) {
            throw new BadRequestException("Quantity is required");
        }
        CartResponse cart = cartService.updateCartItemQuantity(currentUser.getId(), itemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cart updated", cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long itemId) {
        CartResponse cart = cartService.removeCartItem(currentUser.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@CurrentUser UserPrincipal currentUser) {
        cartService.clearCart(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
