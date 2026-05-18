package com.atomquest.portal.service;

import com.atomquest.portal.dto.UserDtos.UpdateUserRequest;
import com.atomquest.portal.entity.Goal;
import com.atomquest.portal.entity.GoalStatus;
import com.atomquest.portal.entity.Quarter;
import com.atomquest.portal.entity.Role;
import com.atomquest.portal.entity.UomType;
import com.atomquest.portal.entity.User;
import com.atomquest.portal.repository.AuditLogRepository;
import com.atomquest.portal.repository.GoalRepository;
import com.atomquest.portal.repository.ManagerCommentRepository;
import com.atomquest.portal.repository.QuarterlyUpdateRepository;
import com.atomquest.portal.repository.SharedGoalRepository;
import com.atomquest.portal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceWorkflowTest {
    @Mock GoalRepository goalRepository;
    @Mock SharedGoalRepository sharedGoalRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock AuditLogRepository auditLogRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock QuarterlyUpdateRepository quarterlyUpdateRepository;
    @Mock ManagerCommentRepository managerCommentRepository;

    @Test
    void achievementExportContainsPlannedTargetActualAchievementAndGovernanceColumns() {
        AdminService adminService = new AdminService(sharedGoalRepository, userRepository, goalRepository, auditService);
        User manager = user(1L, "Manager", Role.MANAGER, "Engineering", null);
        User employee = user(2L, "Employee", Role.EMPLOYEE, "Engineering", manager);
        Goal goal = goal(employee, "Improve Quality", 100.0, 82.0);
        when(goalRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(goal));

        String csv = adminService.exportGoalsToCsv();

        assertThat(csv).contains("Planned Target,Actual Achievement");
        assertThat(csv).contains("\"Employee\",\"Engineering\",\"Manager\"");
        assertThat(csv).contains("100.0,82.0");
        assertThat(csv).contains("\"2026-08-31\",\"No\"");
    }

    @Test
    void adminCanManageReportingHierarchyAndAuditTheChange() {
        UserService userService = new UserService(userRepository, passwordEncoder, auditService);
        User admin = user(1L, "Admin", Role.ADMIN, "HR", null);
        User manager = user(2L, "Manager", Role.MANAGER, "Engineering", admin);
        User employee = user(3L, "Employee", Role.EMPLOYEE, "Engineering", null);
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        var response = userService.update(employee.getId(), new UpdateUserRequest(null, null, manager.getId(), null), admin);

        assertThat(response.managerId()).isEqualTo(manager.getId());
        verify(auditService).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void completionDashboardShowsEmployeeAndManagerCheckInStatus() {
        AnalyticsService analyticsService = new AnalyticsService(goalRepository, quarterlyUpdateRepository, userRepository, managerCommentRepository);
        User manager = user(2L, "Manager", Role.MANAGER, "Engineering", null);
        User employee = user(3L, "Employee", Role.EMPLOYEE, "Engineering", manager);
        when(goalRepository.findAll()).thenReturn(List.of(goal(employee, "Goal", 100.0, 80.0)));
        when(quarterlyUpdateRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of(manager, employee));
        when(userRepository.count()).thenReturn(2L);
        when(userRepository.findByManagerId(manager.getId())).thenReturn(List.of(employee));
        when(quarterlyUpdateRepository.existsByGoalEmployeeIdAndQuarter(employee.getId(), Quarter.Q1)).thenReturn(true);
        when(managerCommentRepository.existsByEmployeeIdAndManagerIdAndQuarter(employee.getId(), manager.getId(), Quarter.Q1)).thenReturn(true);

        Map<String, Object> overview = analyticsService.overview();
        Map<String, Object> completion = (Map<String, Object>) overview.get("checkInCompletion");

        assertThat(completion.get("achievementCompletionRate")).isEqualTo(100L);
        assertThat(completion.get("managerCheckInCompletionRate")).isEqualTo(100L);
        assertThat((List<Map<String, Object>>) completion.get("employeeRows")).hasSize(1);
    }

    private Goal goal(User employee, String title, Double target, Double achievement) {
        return Goal.builder()
                .id(10L)
                .employee(employee)
                .title(title)
                .description("Description")
                .thrustArea("Execution Excellence")
                .uomType(UomType.PERCENT)
                .target(target)
                .achievement(achievement)
                .weightage(100)
                .status(GoalStatus.ON_TRACK)
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
