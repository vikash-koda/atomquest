package com.atomquest.portal.dto;

import com.atomquest.portal.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class UserDtos {
    public record UserResponse(Long id, String name, String email, Role role, Long managerId, String department, Instant createdAt) {}
    public record CreateUserRequest(@NotBlank String name, @Email String email, @NotBlank String password,
                                    @NotNull Role role, Long managerId, @NotBlank String department) {}
    public record UpdateUserRequest(String name, Role role, Long managerId, String department) {}
}
