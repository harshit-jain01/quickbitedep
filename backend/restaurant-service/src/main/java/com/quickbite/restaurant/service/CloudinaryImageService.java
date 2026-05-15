package com.quickbite.restaurant.service;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
public interface CloudinaryImageService {

    String uploadRestaurantImage(MultipartFile imageFile, UUID userId);
}

