package ecommerce.example.Death_Mart.service;

import ecommerce.example.Death_Mart.dto.request.CartItemRequest;
import ecommerce.example.Death_Mart.dto.response.CartItemResponse;
import ecommerce.example.Death_Mart.dto.response.CartResponse;
import ecommerce.example.Death_Mart.entity.Cart;
import ecommerce.example.Death_Mart.entity.CartItem;
import ecommerce.example.Death_Mart.entity.Product;
import ecommerce.example.Death_Mart.entity.User;
import ecommerce.example.Death_Mart.exception.BadRequestException;
import ecommerce.example.Death_Mart.exception.ResourceNotFoundException;
import ecommerce.example.Death_Mart.repository.CartItemRepository;
import ecommerce.example.Death_Mart.repository.CartRepository;
import ecommerce.example.Death_Mart.repository.ProductRepository;
import ecommerce.example.Death_Mart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(Long userId, CartItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BadRequestException("This product is not available");
        }
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new BadRequestException("Cannot add more. Available stock: " + product.getStockQuantity());
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        // Re-fetch to return up-to-date cart
        return mapToResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Transactional
    public CartResponse updateCartItemQuantity(Long userId, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to this user's cart");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            Product product = item.getProduct();
            if (product.getStockQuantity() < quantity) {
                throw new BadRequestException("Requested quantity exceeds stock. Available: " + product.getStockQuantity());
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return mapToResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Transactional
    public CartResponse removeCartItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to this user's cart");
        }

        cartItemRepository.delete(item);

        return mapToResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                Product product = item.getProduct();
                BigDecimal effectivePrice = product.getDiscountPrice() != null &&
                        product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0 ?
                        product.getDiscountPrice() : product.getPrice();

                BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                totalQuantity += item.getQuantity();
                totalAmount = totalAmount.add(subtotal);

                itemResponses.add(CartItemResponse.builder()
                        .id(item.getId())
                        .productId(product.getId())
                        .productName(product.getName())
                        .productImageUrl(product.getImageUrl())
                        .unitPrice(effectivePrice)
                        .quantity(item.getQuantity())
                        .subtotal(subtotal)
                        .build());
            }
        }

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();
    }
}
