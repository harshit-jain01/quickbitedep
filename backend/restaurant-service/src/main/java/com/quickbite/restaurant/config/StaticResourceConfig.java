package com.quickbite.restaurant.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final Path restaurantUploadDirectory;

    public StaticResourceConfig(
            @Value("${app.uploads.restaurants-dir:/tmp/quickbite/uploads/restaurants}") String restaurantUploadDirectory
    ) {
        this.restaurantUploadDirectory = Paths.get(restaurantUploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/v1/restaurants/images/**")
                .addResourceLocations(restaurantUploadDirectory.toUri().toString());
    }
}
