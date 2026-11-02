package ecommerce.example.Death_Mart.service;

import ecommerce.example.Death_Mart.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductImageStorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp");

    private final Path productImagesDirectory;

    public ProductImageStorageService(@Value("${app.uploads-dir:uploads}") String uploadsDirectory) {
        this.productImagesDirectory = Path.of(uploadsDirectory).toAbsolutePath().normalize().resolve("products");
    }

    public String store(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Choose a product image to upload");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("Product images must be 5 MB or smaller");
        }

        String originalFilename = StringUtils.cleanPath(image.getOriginalFilename() == null
                ? "" : image.getOriginalFilename());
        int extensionSeparator = originalFilename.lastIndexOf('.');
        if (extensionSeparator < 0 || extensionSeparator == originalFilename.length() - 1) {
            throw new BadRequestException("Product images must be JPG, JPEG, PNG, or WEBP");
        }

        String extension = originalFilename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        String contentType = EXTENSIONS.get(extension);
        if (contentType == null) {
            throw new BadRequestException("Product images must be JPG, JPEG, PNG, or WEBP");
        }

        byte[] imageBytes = image.getBytes();
        if (imageBytes.length == 0 || imageBytes.length > MAX_IMAGE_SIZE || !matchesImageType(imageBytes, extension)) {
            throw new BadRequestException("The selected file is not a valid " + extension.toUpperCase(Locale.ROOT) + " image");
        }

        Files.createDirectories(productImagesDirectory);
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path destination = productImagesDirectory.resolve(storedFilename).normalize();
        if (!destination.getParent().equals(productImagesDirectory)) {
            throw new IOException("Invalid product image destination");
        }
        Files.write(destination, imageBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        return "/uploads/products/" + storedFilename;
    }

    private boolean matchesImageType(byte[] bytes, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A;
            case "webp" -> bytes.length >= 12
                    && bytes[0] == 'R'
                    && bytes[1] == 'I'
                    && bytes[2] == 'F'
                    && bytes[3] == 'F'
                    && bytes[8] == 'W'
                    && bytes[9] == 'E'
                    && bytes[10] == 'B'
                    && bytes[11] == 'P';
            default -> false;
        };
    }
}
