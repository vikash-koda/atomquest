package com.atomquest.portal.service;

import com.atomquest.portal.dto.AdminDtos.SharedGoalRequest;
import com.atomquest.portal.dto.AdminDtos.PushSharedGoalRequest;
import com.atomquest.portal.entity.*;
import com.atomquest.portal.exception.ApiException;
import com.atomquest.portal.repository.GoalRepository;
import com.atomquest.portal.repository.SharedGoalRepository;
import com.atomquest.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final SharedGoalRepository sharedGoalRepository;
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final AuditService auditService;

    public SharedGoal createSharedGoal(User actor, SharedGoalRequest request) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Owner not found"));
        SharedGoal goal = sharedGoalRepository.save(SharedGoal.builder()
                .title(request.title())
                .description(request.description())
                .thrustArea(request.thrustArea())
                .uomType(request.uomType())
                .target(request.target())
                .deadline(request.deadline())
                .owner(owner)
                .department(request.department())
                .build());
        auditService.log(actor, "CREATE_SHARED_GOAL", "SharedGoal", goal.getId(), null, goal.getTitle());
        return goal;
    }

    @Transactional
    public List<Goal> pushSharedGoal(User actor, PushSharedGoalRequest request) {
        GoalRules.assertWeightageInRange(request.defaultWeightage());
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Owner not found"));
        if (owner.getRole() != Role.EMPLOYEE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Shared goal owner must be an employee");
        }
        if (!owner.getDepartment().equalsIgnoreCase(request.department())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Shared goal owner must belong to the selected department");
        }
        if (actor.getRole() == Role.MANAGER && (owner.getManager() == null || !owner.getManager().getId().equals(actor.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Managers can push goals only to direct reports");
        }

        SharedGoal sharedGoal = sharedGoalRepository.save(SharedGoal.builder()
                .title(request.title())
                .description(request.description())
                .thrustArea(request.thrustArea())
                .uomType(request.uomType())
                .target(request.target())
                .deadline(request.deadline())
                .owner(owner)
                .department(request.department())
                .build());

        Set<Long> recipientIds = new LinkedHashSet<>();
        recipientIds.add(owner.getId());
        if (request.employeeIds() == null || request.employeeIds().isEmpty()) {
            List<User> defaultRecipients = actor.getRole() == Role.MANAGER
                    ? userRepository.findByManagerId(actor.getId())
                    : userRepository.findByDepartmentAndRole(request.department(), Role.EMPLOYEE);
            defaultRecipients.forEach(user -> recipientIds.add(user.getId()));
        } else {
            recipientIds.addAll(request.employeeIds());
        }

        List<Goal> goals = recipientIds.stream()
                .map(userRepository::findById)
                .map(optional -> optional.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient employee not found")))
                .peek(employee -> {
                    if (employee.getRole() != Role.EMPLOYEE) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Shared goals can be pushed only to employees");
                    }
                    if (!employee.getDepartment().equalsIgnoreCase(request.department())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "All shared goal recipients must belong to the selected department");
                    }
                    if (actor.getRole() == Role.MANAGER && (employee.getManager() == null || !employee.getManager().getId().equals(actor.getId()))) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "Managers can push goals only to direct reports");
                    }
                    if (goalRepository.countByEmployeeId(employee.getId()) >= GoalRules.MAX_GOALS_PER_EMPLOYEE) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                                employee.getName() + " already has " + GoalRules.MAX_GOALS_PER_EMPLOYEE + " goals");
                    }
                })
                .map(employee -> Goal.builder()
                        .employee(employee)
                        .title(sharedGoal.getTitle())
                        .description(sharedGoal.getDescription() == null ? request.description() : sharedGoal.getDescription())
                        .thrustArea(sharedGoal.getThrustArea() == null ? request.thrustArea() : sharedGoal.getThrustArea())
                        .uomType(sharedGoal.getUomType() == null ? request.uomType() : sharedGoal.getUomType())
                        .target(sharedGoal.getTarget())
                        .weightage(request.defaultWeightage())
                        .status(GoalStatus.DRAFT)
                        .locked(false)
                        .sharedGoal(sharedGoal)
                        .deadline(sharedGoal.getDeadline() == null ? request.deadline() : sharedGoal.getDeadline())
                        .build())
                .toList();

        List<Goal> saved = goalRepository.saveAll(goals);
        auditService.log(actor, "PUSH_SHARED_GOAL", "SharedGoal", sharedGoal.getId(), null, "recipients=" + saved.size());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SharedGoal> sharedGoals(String department, boolean allowAllWhenBlank) {
        if (department == null || department.isBlank()) {
            if (allowAllWhenBlank) {
                return sharedGoalRepository.findAll();
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Department is required to list shared goals");
        }
        return sharedGoalRepository.findByDepartment(department);
    }

    @Transactional(readOnly = true)
    public String exportGoalsToCsv() {
        List<Goal> goals = goalRepository.findAllByOrderByCreatedAtDesc();
        StringBuilder csv = new StringBuilder();
        csv.append("Employee Name,Department,Manager,Goal Title,Thrust Area,UoM Type,Planned Target,Actual Achievement,Weightage,Status,Deadline,Shared Goal\n");
        for (Goal g : goals) {
            String managerName = g.getEmployee().getManager() != null ? g.getEmployee().getManager().getName() : "";
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%d,\"%s\",\"%s\",\"%s\"\n",
                    g.getEmployee().getName(),
                    g.getEmployee().getDepartment(),
                    managerName,
                    csv(g.getTitle()),
                    g.getThrustArea() != null ? g.getThrustArea() : "",
                    g.getUomType().name(),
                    g.getTarget() != null ? g.getTarget() : "",
                    g.getAchievement() != null ? g.getAchievement() : "",
                    g.getWeightage(),
                    g.getStatus().name(),
                    g.getDeadline(),
                    g.getSharedGoal() == null ? "No" : "Yes"
            ));
        }
        return csv.toString();
    }

    private String csv(String value) {
        return value == null ? "" : value.replace("\"", "\"\"");
    }
}
