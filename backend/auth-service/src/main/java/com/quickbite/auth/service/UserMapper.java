package com.quickbite.auth.service;

import com.quickbite.auth.dto.UserProfileResponse;
import com.quickbite.auth.model.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfile(UserAccount user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getProvider(),
                user.isActive(),
                user.getCreatedAt(),
                user.getProfilePicUrl()
        );
    }
}
