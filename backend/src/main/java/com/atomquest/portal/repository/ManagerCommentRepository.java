package com.atomquest.portal.repository;

import com.atomquest.portal.entity.ManagerComment;
import com.atomquest.portal.entity.Quarter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagerCommentRepository extends JpaRepository<ManagerComment, Long> {
    List<ManagerComment> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<ManagerComment> findByManagerIdOrderByCreatedAtDesc(Long managerId);
    boolean existsByEmployeeIdAndManagerIdAndQuarter(Long employeeId, Long managerId, Quarter quarter);
}
