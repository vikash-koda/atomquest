package com.atomquest.portal.service;

import com.atomquest.portal.dto.AuthDtos.AuthResponse;
import com.atomquest.portal.dto.AuthDtos.LoginRequest;
import com.atomquest.portal.entity.User;
import com.atomquest.portal.repository.UserRepository;
import com.atomquest.portal.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getDepartment());
    }
}
