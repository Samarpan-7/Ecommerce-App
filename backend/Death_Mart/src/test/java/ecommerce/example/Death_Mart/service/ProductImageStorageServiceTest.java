package ecommerce.example.Death_Mart.service;

import ecommerce.example.Death_Mart.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImageStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAllowedImageUnderProductsDirectoryWithoutReplacingExistingFiles() throws IOException {
        ProductImageStorageService storage = new ProductImageStorageService(temporaryDirectory.toString());
        byte[] pngSignature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile image = new MockMultipartFile(
                "image", "product.PNG", "image/png", pngSignature);

        String imageUrl = storage.store(image);

        assertTrue(imageUrl.matches("/uploads/products/[0-9a-f-]{36}\\.png"));
        Path storedFile = temporaryDirectory.resolve("products")
                .resolve(imageUrl.substring(imageUrl.lastIndexOf('/') + 1));
        assertArrayEquals(pngSignature, Files.readAllBytes(storedFile));
        assertFalse(imageUrl.contains("product.PNG"));
    }

    @Test
    void rejectsUnsupportedImageExtensionsAndContent() {
        ProductImageStorageService storage = new ProductImageStorageService(temporaryDirectory.toString());
        MockMultipartFile unsupported = new MockMultipartFile(
                "image", "product.svg", "image/svg+xml", "<svg/>".getBytes());
        MockMultipartFile mismatched = new MockMultipartFile(
                "image", "product.png", "image/png", "not a PNG".getBytes());

        assertThrows(BadRequestException.class, () -> storage.store(unsupported));
        assertThrows(BadRequestException.class, () -> storage.store(mismatched));
    }

    @Test
    void rejectsImagesOverFiveMegabytes() {
        ProductImageStorageService storage = new ProductImageStorageService(temporaryDirectory.toString());
        MockMultipartFile oversized = new MockMultipartFile(
                "image", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertThrows(BadRequestException.class, () -> storage.store(oversized));
    }
}
