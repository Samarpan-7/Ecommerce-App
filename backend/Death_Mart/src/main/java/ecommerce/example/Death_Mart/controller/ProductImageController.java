package ecommerce.example.Death_Mart.controller;

import ecommerce.example.Death_Mart.dto.response.ApiResponse;
import ecommerce.example.Death_Mart.dto.response.ProductImageResponse;
import ecommerce.example.Death_Mart.service.ProductImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/products/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageStorageService productImageStorageService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadProductImage(
            @RequestPart("image") MultipartFile image) throws IOException {
        String imageUrl = productImageStorageService.store(image);
        ProductImageResponse response = ProductImageResponse.builder()
                .imageUrl(imageUrl)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product image uploaded successfully", response));
    }
}
