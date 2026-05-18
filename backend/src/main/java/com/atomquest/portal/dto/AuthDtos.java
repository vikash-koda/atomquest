package com.atomquest.portal.dto;

import com.atomquest.portal.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record LoginRequest(@Email String email, @NotBlank String password) {}
    public record AuthResponse(String token, Long id, String name, String email, Role role, String department) {}
}
