package com.atomquest.portal.repository;

import com.atomquest.portal.entity.Goal;
import com.atomquest.portal.entity.GoalStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    @EntityGraph(attributePaths = {"employee", "sharedGoal"})
    List<Goal> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "sharedGoal"})
    List<Goal> findByEmployeeManagerIdOrderByCreatedAtDesc(Long managerId);

    @EntityGraph(attributePaths = {"employee"})
    List<Goal> findBySharedGoalId(Long sharedGoalId);

    @EntityGraph(attributePaths = {"employee"})
    List<Goal> findAll();

    @EntityGraph(attributePaths = {"employee", "employee.manager", "sharedGoal"})
    List<Goal> findAllByOrderByCreatedAtDesc();

    long countByEmployeeId(Long employeeId);
    long countByStatus(GoalStatus status);
}
