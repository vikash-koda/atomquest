package com.atomquest.portal.controller;

import com.atomquest.portal.dto.AdminDtos.SharedGoalRequest;
import com.atomquest.portal.dto.AdminDtos.PushSharedGoalRequest;
import com.atomquest.portal.dto.GoalDtos.GoalResponse;
import com.atomquest.portal.dto.UserDtos.CreateUserRequest;
import com.atomquest.portal.dto.UserDtos.UpdateUserRequest;
import com.atomquest.portal.dto.UserDtos.UserResponse;
import com.atomquest.portal.entity.AuditLog;
import com.atomquest.portal.entity.SharedGoal;
import com.atomquest.portal.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final GoalService goalService;
    private final AdminService adminService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final CycleService cycleService;

    @GetMapping("/config")
    public java.util.Map<String, Boolean> getConfig() {
        return java.util.Map.of(
            "GOAL_SETTING_WINDOW_OVERRIDE", cycleService.getOverride("GOAL_SETTING_WINDOW_OVERRIDE"),
            "CHECKIN_WINDOW_OVERRIDE_Q1", cycleService.getOverride("CHECKIN_WINDOW_OVERRIDE_Q1"),
            "CHECKIN_WINDOW_OVERRIDE_Q2", cycleService.getOverride("CHECKIN_WINDOW_OVERRIDE_Q2"),
            "CHECKIN_WINDOW_OVERRIDE_Q3", cycleService.getOverride("CHECKIN_WINDOW_OVERRIDE_Q3"),
            "CHECKIN_WINDOW_OVERRIDE_Q4", cycleService.getOverride("CHECKIN_WINDOW_OVERRIDE_Q4")
        );
    }

    @PostMapping("/config/{key}")
    public void setConfig(@PathVariable String key, @RequestParam boolean value) {
        cycleService.setOverride(key, value);
    }

    @GetMapping("/users")
    List<UserResponse> users() {
        return userService.list();
    }

    @PostMapping("/users")
    UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request, currentUserService.get());
    }

    @PatchMapping("/users/{id}")
    UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request, currentUserService.get());
    }

    @PostMapping("/goals/{id}/unlock")
    GoalResponse unlock(@PathVariable Long id) {
        return goalService.unlock(currentUserService.get(), id);
    }

    @GetMapping("/goals")
    List<GoalResponse> goals() {
        return goalService.allGoals();
    }

    @GetMapping(value = "/goals/export", produces = "text/csv")
    public ResponseEntity<String> exportGoals() {
        String csv = adminService.exportGoalsToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"goals-export.csv\"")
                .body(csv);
    }

    @GetMapping("/shared-goals")
    List<SharedGoal> sharedGoals(@RequestParam(required = false) String department) {
        return adminService.sharedGoals(department, true);
    }

    @PostMapping("/shared-goals")
    SharedGoal createSharedGoal(@Valid @RequestBody SharedGoalRequest request) {
        return adminService.createSharedGoal(currentUserService.get(), request);
    }

    @PostMapping("/shared-goals/push")
    List<GoalResponse> pushSharedGoal(@Valid @RequestBody PushSharedGoalRequest request) {
        return adminService.pushSharedGoal(currentUserService.get(), request).stream()
                .map(goalService::toResponse)
                .toList();
    }

    @GetMapping("/audit-logs")
    Page<AuditLog> auditLogs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return auditService.list(PageRequest.of(page, size));
    }
}
