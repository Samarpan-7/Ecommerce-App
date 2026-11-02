package ecommerce.example.Death_Mart.service;

import ecommerce.example.Death_Mart.dto.request.AddressRequest;
import ecommerce.example.Death_Mart.dto.request.OrderRequest;
import ecommerce.example.Death_Mart.dto.response.*;
import ecommerce.example.Death_Mart.entity.*;
import ecommerce.example.Death_Mart.exception.BadRequestException;
import ecommerce.example.Death_Mart.exception.ResourceNotFoundException;
import ecommerce.example.Death_Mart.repository.*;
import ecommerce.example.Death_Mart.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressService addressService;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty. Add products before placing an order.");
        }

        // Resolve shipping address
        Address address = null;
        if (request.getShippingAddressId() != null) {
            address = addressRepository.findByIdAndUserId(request.getShippingAddressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getShippingAddressId()));
        } else if (request.getShippingAddress() != null) {
            AddressResponse savedAddrResp = addressService.addAddress(userId, request.getShippingAddress());
            address = addressRepository.findById(savedAddrResp.getId()).orElse(null);
        } else {
            // Find default address
            List<Address> addresses = addressRepository.findByUserId(userId);
            address = addresses.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                    .findFirst()
                    .orElse(!addresses.isEmpty() ? addresses.get(0) : null);
        }

        if (address == null) {
            throw new BadRequestException("A shipping address is required to place an order.");
        }

        // Deduct inventory and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Product '" + product.getName() + "' does not have enough stock. Available: " + product.getStockQuantity());
            }

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal unitPrice = product.getDiscountPrice() != null &&
                    product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0 ?
                    product.getDiscountPrice() : product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);
        }

        // Build and save order
        Order order = Order.builder()
                .user(user)
                .shippingAddress(address)
                .orderNumber(OrderNumberGenerator.generateOrderNumber())
                .totalAmount(totalAmount)
                .status(OrderStatus.PROCESSING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // Associate order items
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        // Record payment
        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.COMPLETED)
                .amount(totalAmount)
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        savedOrder.setPayment(savedPayment);

        // Clear cart
        cartItemRepository.deleteByCartId(cart.getId());

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable);

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrdersAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // If transitioning to CANCELLED, restore stock
        if (status == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
                productRepository.save(p);
            }
        }

        order.setStatus(status);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    public OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                itemResponses.add(OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(product != null ? product.getId() : null)
                        .productName(product != null ? product.getName() : "Unknown Product")
                        .productImageUrl(product != null ? product.getImageUrl() : null)
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build());
            }
        }

        PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            paymentResponse = PaymentResponse.builder()
                    .id(order.getPayment().getId())
                    .paymentMethod(order.getPayment().getPaymentMethod())
                    .paymentStatus(order.getPayment().getPaymentStatus())
                    .amount(order.getPayment().getAmount())
                    .paymentDate(order.getPayment().getPaymentDate())
                    .build();
        }

        AddressResponse addressResponse = null;
        if (order.getShippingAddress() != null) {
            Address a = order.getShippingAddress();
            addressResponse = AddressResponse.builder()
                    .id(a.getId())
                    .street(a.getStreet())
                    .city(a.getCity())
                    .state(a.getState())
                    .zipCode(a.getZipCode())
                    .country(a.getCountry())
                    .isDefault(a.getIsDefault())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .userFullName(order.getUser() != null ? order.getUser().getFullName() : null)
                .shippingAddress(addressResponse)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemResponses)
                .payment(paymentResponse)
                .orderDate(order.getOrderDate())
                .build();
    }
}
