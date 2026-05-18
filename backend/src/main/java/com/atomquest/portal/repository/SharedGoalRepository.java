package com.atomquest.portal.repository;

import com.atomquest.portal.entity.SharedGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedGoalRepository extends JpaRepository<SharedGoal, Long> {
    List<SharedGoal> findByDepartment(String department);
}
