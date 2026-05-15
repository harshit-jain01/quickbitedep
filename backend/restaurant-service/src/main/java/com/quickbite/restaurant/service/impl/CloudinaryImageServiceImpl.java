package com.quickbite.restaurant.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.quickbite.restaurant.service.CloudinaryImageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryImageServiceImpl implements CloudinaryImageService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryImageServiceImpl.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Cloudinary cloudinary;
    private final String folder;

    public CloudinaryImageServiceImpl(
            ObjectProvider<Cloudinary> cloudinaryProvider,
            @Value("${cloudinary.folder:quickbite/restaurants}") String folder,
            @Value("${cloudinary.enabled:false}") boolean enabled,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        this.cloudinary = resolveCloudinary(cloudinaryProvider.getIfAvailable(), enabled, cloudName, apiKey, apiSecret);
        this.folder = folder;
    }

    private Cloudinary resolveCloudinary(Cloudinary existing, boolean enabled, String cloudName, String apiKey, String apiSecret) {
        if (existing != null) {
            return existing;
        }

        Map<String, String> dotenv = readDotEnvValues();
        String resolvedCloudName = firstNonBlank(cloudName, System.getenv("CLOUDINARY_CLOUD_NAME"), dotenv.get("CLOUDINARY_CLOUD_NAME"));
        String resolvedApiKey = firstNonBlank(apiKey, System.getenv("CLOUDINARY_API_KEY"), dotenv.get("CLOUDINARY_API_KEY"));
        String resolvedApiSecret = firstNonBlank(apiSecret, System.getenv("CLOUDINARY_API_SECRET"), dotenv.get("CLOUDINARY_API_SECRET"));
        String envEnabled = firstNonBlank(System.getenv("CLOUDINARY_ENABLED"), dotenv.get("CLOUDINARY_ENABLED"));

        boolean hasCredentials = isNotBlank(resolvedCloudName) && isNotBlank(resolvedApiKey) && isNotBlank(resolvedApiSecret);
        boolean resolvedEnabled = enabled || "true".equalsIgnoreCase(envEnabled) || (isBlank(envEnabled) && hasCredentials);
        if (!resolvedEnabled || !hasCredentials) {
            return null;
        }

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", resolvedCloudName,
                "api_key", resolvedApiKey,
                "api_secret", resolvedApiSecret,
                "secure", true
        ));
    }

    private Map<String, String> readDotEnvValues() {
        Map<String, String> values = new LinkedHashMap<>();
        Path[] candidates = new Path[] {
                Paths.get(".env"),
                Paths.get("restaurant-service", ".env"),
                Paths.get(System.getProperty("user.dir", "."), ".env"),
                Paths.get(System.getProperty("user.dir", "."), "restaurant-service", ".env")
        };

        for (Path path : candidates) {
            if (!Files.exists(path)) {
                continue;
            }

            try {
                for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String line = rawLine == null ? "" : rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int separator = line.indexOf('=');
                    if (separator <= 0) {
                        continue;
                    }
                    String key = line.substring(0, separator).trim();
                    String value = stripQuotes(line.substring(separator + 1).trim());
                    if (!key.isEmpty() && !values.containsKey(key)) {
                        values.put(key, value);
                    }
                }
            } catch (IOException ignored) {
                // Ignore parse failures and continue with other sources.
            }
        }

        return values;
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (isNotBlank(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    @Override
    public String uploadRestaurantImage(MultipartFile imageFile, UUID userId) {
        logger.info("Uploading restaurant image for userId={}", userId);
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Restaurant image is required");
        }

        validateImageFormat(imageFile);

        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is disabled. Set CLOUDINARY_ENABLED=true and provide CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }

        try {
            String originalFilename = imageFile.getOriginalFilename() == null ? "restaurant-image" : imageFile.getOriginalFilename();
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    imageFile.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", "restaurant_" + userId,
                            "overwrite", true,
                            "resource_type", "image",
                            "use_filename", true,
                            "filename_override", originalFilename
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new IllegalStateException("Cloudinary upload did not return a URL");
            }

            return secureUrl.toString();
        } catch (IOException ioException) {
            logger.error("Failed to read restaurant image file for userId={}", userId, ioException);
            throw new RuntimeException("Failed to read image file", ioException);
        } catch (Exception exception) {
            logger.error("Failed to upload restaurant image for userId={}", userId, exception);
            throw new RuntimeException("Failed to upload image to Cloudinary: " + exception.getMessage(), exception);
        }
    }

    private void validateImageFormat(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        String extension = getFileExtension(imageFile.getOriginalFilename());

        boolean allowedType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean allowedExtension = extension != null && ALLOWED_EXTENSIONS.contains(extension);

        if (!allowedType && !allowedExtension) {
            throw new IllegalArgumentException("Unsupported image format. Please upload JPG, PNG, or WEBP.");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}

