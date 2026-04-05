package com.krunity.HostelManagment.Mapper;


import com.krunity.HostelManagment.dto.CreateUserRequest;
import com.krunity.HostelManagment.dto.UserResponse;
import com.krunity.HostelManagment.model.Role;
import com.krunity.HostelManagment.model.User;

public class UserMapper {

    public static User toEntity(CreateUserRequest createUserRequest, Role role) {
        return User.builder()
        .displayName(createUserRequest.getDisplayName())
        .username(createUserRequest.getUsername())
        .passwordHash(createUserRequest.getPassword())
        .phoneNumber(createUserRequest.getPhoneNumber())
        .role(role)
        .isActive(createUserRequest.getIsActive())
        .build();
    }
    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .displayName(user.getDisplayName())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .isActive(user.isActive())
                .build();
    }
}
