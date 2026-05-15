package com.quickbite.menu.service;

import org.springframework.web.multipart.MultipartFile;
public interface CloudinaryImageService {

    String uploadMenuItemImage(MultipartFile imageFile, Long restaurantId);
}

