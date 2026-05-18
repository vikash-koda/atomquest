package com.atomquest.portal.service;

import com.atomquest.portal.dto.AdminDtos.PushSharedGoalRequest;
import com.atomquest.portal.dto.GoalDtos.GoalRequest;
import com.atomquest.portal.dto.GoalDtos.ReviewGoalRequest;
import com.atomquest.portal.dto.GoalDtos.UpdateGoalRequest;
import com.atomquest.portal.entity.Goal;
import com.atomquest.portal.entity.GoalStatus;
import com.atomquest.portal.entity.Role;
import com.atomquest.portal.entity.SharedGoal;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase1WorkflowTest {
    @Mock GoalRepository goalRepository;
    @Mock SharedGoalRepository sharedGoalRepository;
    @Mock QuarterlyUpdateRepository quarterlyUpdateRepository;
    @Mock ManagerCommentRepository managerCommentRepository;
    @Mock AuditService auditService;
    @Mock CycleService cycleService;
    @Mock UserRepository userRepository;

    GoalService goalService;

    User admin;
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

        admin = user(1L, "Admin", Role.ADMIN, "Engineering", null);
        manager = user(2L, "Manager", Role.MANAGER, "Engineering", admin);
        employee = user(3L, "Employee", Role.EMPLOYEE, "Engineering", manager);
    }

    @Test
    void employeeGoalCreationEnforcesWindowMaxGoalsAndMinimumWeightage() {
        when(cycleService.isGoalSettingWindowOpen()).thenReturn(true);
        when(goalRepository.countByEmployeeId(employee.getId())).thenReturn(8L);

        assertThatThrownBy(() -> goalService.create(employee, goalRequest("Goal", 10)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Maximum 8 goals");

        when(goalRepository.countByEmployeeId(employee.getId())).thenReturn(0L);

        assertThatThrownBy(() -> goalService.create(employee, goalRequest("Goal", 9)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Weightage must be between 10% and 100%");
    }

    @Test
    void submissionRequiresTotalWeightageToEqual100AndMovesDraftGoalsToSubmitted() {
        when(cycleService.isGoalSettingWindowOpen()).thenReturn(true);

        Goal first = goal(employee, "First", 40, GoalStatus.DRAFT, false);
        Goal second = goal(employee, "Second", 50, GoalStatus.DRAFT, false);
        when(goalRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> goalService.submit(employee))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Total weightage must equal 100%");

        second.setWeightage(60);
        goalService.submit(employee);

        assertThat(first.getStatus()).isEqualTo(GoalStatus.SUBMITTED);
        assertThat(second.getStatus()).isEqualTo(GoalStatus.SUBMITTED);
    }

    @Test
    void managerCanEditTargetAndWeightageDuringApprovalAndApprovalLocksGoal() {
        Goal submitted = goal(employee, "Submitted", 40, GoalStatus.SUBMITTED, false);
        Goal peer = goal(employee, "Peer", 50, GoalStatus.SUBMITTED, false);
        when(goalRepository.findById(submitted.getId())).thenReturn(Optional.of(submitted));
        when(goalRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()))
                .thenReturn(List.of(submitted, peer));

        goalService.review(manager, submitted.getId(), new ReviewGoalRequest(
                GoalStatus.APPROVED,
                "Looks good",
                120.0,
                50));

        assertThat(submitted.getTarget()).isEqualTo(120.0);
        assertThat(submitted.getWeightage()).isEqualTo(50);
        assertThat(submitted.getStatus()).isEqualTo(GoalStatus.APPROVED);
        assertThat(submitted.isLocked()).isTrue();
    }

    @Test
    void managerCanReturnSubmittedGoalForEmployeeRework() {
        Goal submitted = goal(employee, "Submitted", 100, GoalStatus.SUBMITTED, false);
        when(goalRepository.findById(submitted.getId())).thenReturn(Optional.of(submitted));

        goalService.review(manager, submitted.getId(), new ReviewGoalRequest(
                GoalStatus.REJECTED,
                "Revise target",
                null,
                null));

        assertThat(submitted.getStatus()).isEqualTo(GoalStatus.REJECTED);
        assertThat(submitted.isLocked()).isFalse();
    }

    @Test
    void sharedGoalRecipientCanChangeOnlyWeightage() {
        SharedGoal sharedGoal = sharedGoal(admin);
        Goal sharedRecipientGoal = goal(employee, sharedGoal.getTitle(), 25, GoalStatus.DRAFT, false);
        sharedRecipientGoal.setSharedGoal(sharedGoal);
        sharedRecipientGoal.setTarget(sharedGoal.getTarget());
        when(goalRepository.findById(sharedRecipientGoal.getId())).thenReturn(Optional.of(sharedRecipientGoal));

        assertThatThrownBy(() -> goalService.update(employee, sharedRecipientGoal.getId(), new UpdateGoalRequest(
                "Changed title",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Recipients can edit only weightage");

        goalService.update(employee, sharedRecipientGoal.getId(), new UpdateGoalRequest(
                null,
                null,
                null,
                null,
                null,
                30,
                null,
                null,
                null));

        assertThat(sharedRecipientGoal.getWeightage()).isEqualTo(30);
        assertThat(sharedRecipientGoal.getTitle()).isEqualTo(sharedGoal.getTitle());
        assertThat(sharedRecipientGoal.getTarget()).isEqualTo(sharedGoal.getTarget());
    }

    @Test
    void managerPushesSharedKpiToDirectReportsWithReadOnlyDefinitionCopied() {
        AdminService adminService = new AdminService(sharedGoalRepository, userRepository, goalRepository, auditService);
        User otherReport = user(4L, "Other Report", Role.EMPLOYEE, "Engineering", manager);
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(userRepository.findById(otherReport.getId())).thenReturn(Optional.of(otherReport));
        when(userRepository.findByManagerId(manager.getId())).thenReturn(List.of(employee, otherReport));
        when(goalRepository.countByEmployeeId(employee.getId())).thenReturn(0L);
        when(goalRepository.countByEmployeeId(otherReport.getId())).thenReturn(0L);
        when(sharedGoalRepository.save(any(SharedGoal.class))).thenAnswer(invocation -> {
            SharedGoal saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        when(goalRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Goal> pushed = adminService.pushSharedGoal(manager, new PushSharedGoalRequest(
                "Department reliability",
                "Reliability KPI",
                "Operational Excellence",
                UomType.PERCENT,
                99.5,
                employee.getId(),
                "Engineering",
                20,
                LocalDate.of(2026, 8, 31),
                List.of()));

        assertThat(pushed).hasSize(2);
        assertThat(pushed).allSatisfy(goal -> {
            assertThat(goal.getTitle()).isEqualTo("Department reliability");
            assertThat(goal.getDescription()).isEqualTo("Reliability KPI");
            assertThat(goal.getThrustArea()).isEqualTo("Operational Excellence");
            assertThat(goal.getUomType()).isEqualTo(UomType.PERCENT);
            assertThat(goal.getTarget()).isEqualTo(99.5);
            assertThat(goal.getDeadline()).isEqualTo(LocalDate.of(2026, 8, 31));
            assertThat(goal.getWeightage()).isEqualTo(20);
            assertThat(goal.getStatus()).isEqualTo(GoalStatus.DRAFT);
            assertThat(goal.getSharedGoal()).isNotNull();
        });

        ArgumentCaptor<SharedGoal> sharedGoalCaptor = ArgumentCaptor.forClass(SharedGoal.class);
        verify(sharedGoalRepository).save(sharedGoalCaptor.capture());
        assertThat(sharedGoalCaptor.getValue().getOwner()).isEqualTo(employee);
    }

    @Test
    void managerCannotChangeSharedGoalTargetDuringApproval() {
        SharedGoal sharedGoal = sharedGoal(admin);
        Goal submitted = goal(employee, sharedGoal.getTitle(), 100, GoalStatus.SUBMITTED, false);
        submitted.setSharedGoal(sharedGoal);
        when(goalRepository.findById(submitted.getId())).thenReturn(Optional.of(submitted));

        assertThatThrownBy(() -> goalService.review(manager, submitted.getId(), new ReviewGoalRequest(
                GoalStatus.APPROVED,
                null,
                120.0,
                100)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Shared goal target is read-only");
    }

    private GoalRequest goalRequest(String title, int weightage) {
        return new GoalRequest(
                title,
                "Description",
                "Execution Excellence",
                UomType.PERCENT,
                100.0,
                weightage,
                LocalDate.of(2026, 8, 31),
                null);
    }

    private Goal goal(User owner, String title, int weightage, GoalStatus status, boolean locked) {
        Goal goal = Goal.builder()
                .id(Math.abs((long) title.hashCode()))
                .employee(owner)
                .title(title)
                .description("Description")
                .thrustArea("Execution Excellence")
                .uomType(UomType.PERCENT)
                .target(100.0)
                .weightage(weightage)
                .status(status)
                .locked(locked)
                .deadline(LocalDate.of(2026, 8, 31))
                .build();
        return goal;
    }

    private SharedGoal sharedGoal(User owner) {
        return SharedGoal.builder()
                .id(10L)
                .title("Shared KPI")
                .description("Shared description")
                .thrustArea("Operational Excellence")
                .uomType(UomType.PERCENT)
                .target(99.5)
                .deadline(LocalDate.of(2026, 8, 31))
                .owner(owner)
                .department("Engineering")
                .build();
    }

    private User user(Long id, String name, Role role, String department, User manager) {
        return User.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase().replace(" ", ".") + "@demo.com")
                .password("encoded")
                .role(role)
                .manager(manager)
                .department(department)
                .build();
    }
}
