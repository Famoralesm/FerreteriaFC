package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.UserResponse;
import com.ferreteria.fc.persistence.entity.User;

public class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {

        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .avatarUrl(
                        user.getAvatarPath() == null
                                ? null
                                : "/api/media/" + user.getAvatarPath()
                )
                .build();
    }

}