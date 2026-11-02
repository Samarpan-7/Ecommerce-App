package ecommerce.example.Death_Mart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ProductImageResourceConfig implements WebMvcConfigurer {

    private final Path productImagesDirectory;

    public ProductImageResourceConfig(@Value("${app.uploads-dir:uploads}") String uploadsDirectory) {
        this.productImagesDirectory = Path.of(uploadsDirectory).toAbsolutePath().normalize().resolve("products");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = productImagesDirectory.toUri().toString();
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
