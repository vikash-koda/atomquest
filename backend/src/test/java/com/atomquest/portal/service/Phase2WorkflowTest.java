package com.atomquest.portal.service;

import com.atomquest.portal.dto.GoalDtos.QuarterUpdateRequest;
import com.atomquest.portal.entity.Goal;
import com.atomquest.portal.entity.GoalStatus;
import com.atomquest.portal.entity.ManagerComment;
import com.atomquest.portal.entity.ProgressStatus;
import com.atomquest.portal.entity.Quarter;
import com.atomquest.portal.entity.QuarterlyUpdate;
import com.atomquest.portal.entity.Role;
import com.atomquest.portal.entity.UomType;
import com.atomquest.portal.entity.User;
import com.atomquest.portal.exception.ApiException;
import com.atomquest.portal.repository.GoalRepository;
import com.atomquest.portal.repository.ManagerCommentRepository;
import com.atomquest.portal.repository.QuarterlyUpdateRepository;
import com.atomquest.portal.repository.SharedGoalRepository;
import com.atomquest.portal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase2WorkflowTest {
    @Mock GoalRepository goalRepository;
    @Mock SharedGoalRepository sharedGoalRepository;
    @Mock QuarterlyUpdateRepository quarterlyUpdateRepository;
    @Mock ManagerCommentRepository managerCommentRepository;
    @Mock AuditService auditService;
    @Mock CycleService cycleService;
    @Mock UserRepository userRepository;

    GoalService goalService;
    User manager;
    User employee;

    @BeforeEach
    void setUp() {
        goalService = new GoalService(
                goalRepository,
                sharedGoalRepository,
                quarterlyUpdateRepository,
                managerCommentRepository,
                auditService,
                cycleService,
                userRepository);
        lenient().when(quarterlyUpdateRepository.findTopByGoalIdOrderByUpdatedAtDesc(any())).thenReturn(Optional.empty());

        manager = user(2L, "Manager", Role.MANAGER, "Engineering", null);
        employee = user(3L, "Employee", Role.EMPLOYEE, "Engineering", manager);
    }

    @Test
    void quarterlyUpdateIsBlockedOutsideActiveQuarterWindow() {
        Goal goal = approvedGoal(UomType.PERCENT, 100.0);
        when(cycleService.isCheckInWindowOpen(Quarter.Q1)).thenReturn(false);

        assertThatThrownBy(() -> goalService.addQuarterlyUpdate(employee, goal.getId(), update(Quarter.Q1, 75.0, ProgressStatus.ON_TRACK, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("active check-in period");
    }

    @Test
    void quarterlyUpdateStoresActualStatusCommentAndMinProgressScore() {
        Goal goal = approvedGoal(UomType.PERCENT, 100.0);
        when(cycleService.isCheckInWindowOpen(Quarter.Q1)).thenReturn(true);
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(quarterlyUpdateRepository.findByGoalIdAndQuarter(goal.getId(), Quarter.Q1)).thenReturn(Optional.empty());

        goalService.addQuarterlyUpdate(employee, goal.getId(), update(Quarter.Q1, 75.0, ProgressStatus.ON_TRACK, null));

        ArgumentCaptor<QuarterlyUpdate> captor = ArgumentCaptor.forClass(QuarterlyUpdate.class);
        verify(quarterlyUpdateRepository).save(captor.capture());
        QuarterlyUpdate saved = captor.getValue();
        assertThat(saved.getAchievement()).isEqualTo(75.0);
        assertThat(saved.getProgressScore()).isEqualTo(75.0);
        assertThat(saved.getStatus()).isEqualTo(ProgressStatus.ON_TRACK);
        assertThat(saved.getComment()).isEqualTo("Quarterly progress");
        assertThat(goal.getAchievement()).isEqualTo(75.0);
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ON_TRACK);
    }

    @Test
    void quarterlyUpdateComputesMaxZeroAndTimelineScores() {
        assertProgress(UomType.MAX, 10.0, 5.0, null, 100.0);
        assertProgress(UomType.ZERO, 0.0, 1.0, null, 0.0);
        assertProgress(UomType.ZERO, 0.0, 0.0, null, 100.0);
        assertProgress(UomType.TIMELINE, 1.0, 1.0, LocalDate.of(2026, 8, 30), 100.0);
        assertProgress(UomType.TIMELINE, 1.0, 1.0, LocalDate.of(2026, 9, 1), 0.0);
    }

    @Test
    void managerCanStoreStructuredCheckInCommentForDirectReport() {
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(cycleService.isCheckInWindowOpen(Quarter.Q2)).thenReturn(true);
        when(managerCommentRepository.save(any(ManagerComment.class))).thenAnswer(invocation -> {
            ManagerComment comment = invocation.getArgument(0);
            comment.setId(100L);
            return comment;
        });

        var response = goalService.addManagerComment(manager, employee.getId(), Quarter.Q2, "Discussed blockers and next steps.");

        assertThat(response.employeeId()).isEqualTo(employee.getId());
        assertThat(response.employeeName()).isEqualTo(employee.getName());
        assertThat(response.managerName()).isEqualTo(manager.getName());
        assertThat(response.quarter()).isEqualTo("Q2");
        assertThat(response.comment()).isEqualTo("Discussed blockers and next steps.");
    }

    private void assertProgress(UomType uomType, double target, double achievement, LocalDate completionDate, double expectedScore) {
        Goal goal = approvedGoal(uomType, target);
        when(cycleService.isCheckInWindowOpen(Quarter.Q3)).thenReturn(true);
        when(goalRepository.findById(goal.getId())).thenReturn(Optional.of(goal));
        when(quarterlyUpdateRepository.findByGoalIdAndQuarter(goal.getId(), Quarter.Q3)).thenReturn(Optional.empty());

        goalService.addQuarterlyUpdate(employee, goal.getId(), update(Quarter.Q3, achievement, ProgressStatus.COMPLETED, completionDate));

        ArgumentCaptor<QuarterlyUpdate> captor = ArgumentCaptor.forClass(QuarterlyUpdate.class);
        verify(quarterlyUpdateRepository).save(captor.capture());
        assertThat(captor.getValue().getProgressScore()).isEqualTo(expectedScore);
        clearInvocations(quarterlyUpdateRepository, goalRepository, cycleService);
    }

    private QuarterUpdateRequest update(Quarter quarter, double achievement, ProgressStatus status, LocalDate completionDate) {
        return new QuarterUpdateRequest(quarter, achievement, "Quarterly progress", null, status, completionDate);
    }

    private Goal approvedGoal(UomType uomType, double target) {
        return Goal.builder()
                .id(Math.abs((long) uomType.name().hashCode() + System.nanoTime()))
                .employee(employee)
                .title(uomType.name() + " goal")
                .description("Description")
                .thrustArea("Execution Excellence")
                .uomType(uomType)
                .target(target)
                .weightage(100)
                .status(GoalStatus.APPROVED)
                .locked(true)
                .deadline(LocalDate.of(2026, 8, 31))
                .build();
    }

    private User user(Long id, String name, Role role, String department, User manager) {
        return User.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase() + "@demo.com")
                .password("encoded")
                .role(role)
                .manager(manager)
                .department(department)
                .build();
    }
}
