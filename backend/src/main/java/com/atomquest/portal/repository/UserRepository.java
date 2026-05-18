package com.atomquest.portal.repository;

import com.atomquest.portal.entity.Role;
import com.atomquest.portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByManagerId(Long managerId);
    List<User> findByDepartmentAndRole(String department, Role role);
    long countByRole(Role role);
}
