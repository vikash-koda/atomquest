package com.atomquest.portal.controller;

import com.atomquest.portal.dto.AdminDtos.PushSharedGoalRequest;
import com.atomquest.portal.dto.GoalDtos.GoalResponse;
import com.atomquest.portal.dto.GoalDtos.ManagerCommentResponse;
import com.atomquest.portal.dto.GoalDtos.ReviewGoalRequest;
import com.atomquest.portal.dto.UserDtos.UserResponse;
import com.atomquest.portal.entity.Quarter;
import com.atomquest.portal.service.AdminService;
import com.atomquest.portal.service.CurrentUserService;
import com.atomquest.portal.service.GoalService;
import com.atomquest.portal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {
    private final GoalService goalService;
    private final AdminService adminService;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    @GetMapping("/team-goals")
    List<GoalResponse> teamGoals() {
        return goalService.teamGoals(currentUserService.get());
    }

    @GetMapping("/team-users")
    List<UserResponse> teamUsers() {
        return userService.directReports(currentUserService.get());
    }

    @PostMapping("/goals/{id}/review")
    GoalResponse review(@PathVariable Long id, @Valid @RequestBody ReviewGoalRequest request) {
        return goalService.review(currentUserService.get(), id, request);
    }

    @PostMapping("/shared-goals/push")
    List<GoalResponse> pushSharedGoal(@Valid @RequestBody PushSharedGoalRequest request) {
        return adminService.pushSharedGoal(currentUserService.get(), request).stream()
                .map(goalService::toResponse)
                .toList();
    }

    public record AddCommentRequest(Long employeeId, Quarter quarter, String comment) {}

    @PostMapping("/comments")
    ManagerCommentResponse addComment(@Valid @RequestBody AddCommentRequest request) {
        return goalService.addManagerComment(currentUserService.get(), request.employeeId(), request.quarter(), request.comment());
    }

    @GetMapping("/comments")
    List<ManagerCommentResponse> comments() {
        return goalService.managerCommentsByManager(currentUserService.get());
    }
}
