package com.atomquest.portal.service;

import com.atomquest.portal.dto.UserDtos.CreateUserRequest;
import com.atomquest.portal.dto.UserDtos.UpdateUserRequest;
import com.atomquest.portal.dto.UserDtos.UserResponse;
import com.atomquest.portal.entity.User;
import com.atomquest.portal.exception.ApiException;
import com.atomquest.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> directReports(User manager) {
        return userRepository.findByManagerId(manager.getId()).stream().map(this::toResponse).toList();
    }

    public UserResponse create(CreateUserRequest request, User actor) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }
        User manager = request.managerId() == null ? null : find(request.managerId());
        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .manager(manager)
                .department(request.department())
                .build());
        auditService.log(actor, "CREATE_USER", "User", user.getId(), null, user.getEmail());
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, User actor) {
        User user = find(id);
        String oldValue = "name=" + user.getName() + ",role=" + user.getRole()
                + ",managerId=" + (user.getManager() == null ? "" : user.getManager().getId())
                + ",department=" + user.getDepartment();
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.managerId() != null) {
            if (request.managerId() == 0) {
                user.setManager(null);
            } else {
            if (request.managerId().equals(user.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "User cannot be their own manager");
            }
            user.setManager(find(request.managerId()));
            }
        }
        if (request.department() != null && !request.department().isBlank()) {
            user.setDepartment(request.department().trim());
        }
        String newValue = "name=" + user.getName() + ",role=" + user.getRole()
                + ",managerId=" + (user.getManager() == null ? "" : user.getManager().getId())
                + ",department=" + user.getDepartment();
        auditService.log(actor, "UPDATE_USER", "User", user.getId(), oldValue, newValue);
        return toResponse(user);
    }

    public User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getManager() == null ? null : user.getManager().getId(), user.getDepartment(), user.getCreatedAt());
    }
}
