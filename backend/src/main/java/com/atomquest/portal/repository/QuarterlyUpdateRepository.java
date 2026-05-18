package com.atomquest.portal.repository;

import com.atomquest.portal.entity.Quarter;
import com.atomquest.portal.entity.QuarterlyUpdate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuarterlyUpdateRepository extends JpaRepository<QuarterlyUpdate, Long> {
    Optional<QuarterlyUpdate> findByGoalIdAndQuarter(Long goalId, Quarter quarter);

    Optional<QuarterlyUpdate> findTopByGoalIdOrderByUpdatedAtDesc(Long goalId);

    @EntityGraph(attributePaths = {"goal"})
    List<QuarterlyUpdate> findByGoalEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {"goal"})
    List<QuarterlyUpdate> findAll();

    boolean existsByGoalEmployeeIdAndQuarter(Long employeeId, Quarter quarter);
}
