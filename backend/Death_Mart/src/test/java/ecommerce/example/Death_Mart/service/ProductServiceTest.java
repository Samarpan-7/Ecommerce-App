package ecommerce.example.Death_Mart.service;

import ecommerce.example.Death_Mart.dto.request.ProductRequest;
import ecommerce.example.Death_Mart.dto.response.PagedResponse;
import ecommerce.example.Death_Mart.dto.response.ProductResponse;
import ecommerce.example.Death_Mart.entity.Category;
import ecommerce.example.Death_Mart.entity.Product;
import ecommerce.example.Death_Mart.repository.CategoryRepository;
import ecommerce.example.Death_Mart.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createsProductInRepositoryWithItsExistingCategory() {
        Category category = Category.builder()
                .id(3L)
                .name("Stationery")
                .build();
        ProductRequest request = ProductRequest.builder()
                .name("  New Quill  ")
                .description("A writing tool")
                .price(new BigDecimal("19.99"))
                .stockQuantity(4)
                .imageUrl("https://example.com/quill.png")
                .categoryId(3L)
                .build();
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(12L);
            return product;
        });

        ProductResponse response = productService.createProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertEquals("New Quill", savedProduct.getName());
        assertEquals("A writing tool", savedProduct.getDescription());
        assertEquals(new BigDecimal("19.99"), savedProduct.getPrice());
        assertEquals(4, savedProduct.getStockQuantity());
        assertEquals("https://example.com/quill.png", savedProduct.getImageUrl());
        assertEquals(category, savedProduct.getCategory());
        assertEquals(12L, response.getId());
        assertEquals("Stationery", response.getCategoryName());
    }

    @Test
    void filtersAndSortsProductsUsingTheRequestedCatalogOptions() {
        Category category = Category.builder()
                .id(3L)
                .name("Stationery")
                .build();
        Product product = Product.builder()
                .id(12L)
                .name("Quill")
                .description("A writing tool")
                .price(new BigDecimal("19.99"))
                .stockQuantity(4)
                .imageUrl("https://example.com/quill.png")
                .isActive(true)
                .category(category)
                .build();
        Pageable pageable = PageRequest.of(0, 12, Sort.by("price").descending());
        when(productRepository.searchByCategoryIdAndKeyword(eq(3L), eq("quill"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        PagedResponse<ProductResponse> response =
                productService.getAllProducts(3L, " quill ", 0, 12, "price", "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).searchByCategoryIdAndKeyword(eq(3L), eq("quill"), pageableCaptor.capture());
        assertEquals(Sort.by("price").descending(), pageableCaptor.getValue().getSort());
        assertEquals("Quill", response.getContent().get(0).getName());
        assertEquals("Stationery", response.getContent().get(0).getCategoryName());
        assertEquals(new BigDecimal("19.99"), response.getContent().get(0).getPrice());
        assertEquals(4, response.getContent().get(0).getStockQuantity());
    }

    @Test
    void deleteProductDeactivatesItInsteadOfRemovingOrderHistory() {
        Product product = Product.builder()
                .id(12L)
                .isActive(true)
                .build();
        when(productRepository.findById(12L)).thenReturn(Optional.of(product));

        productService.deleteProduct(12L);

        assertFalse(product.getIsActive());
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(product);
    }
}
