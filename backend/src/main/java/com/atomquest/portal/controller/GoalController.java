package com.atomquest.portal.controller;

import com.atomquest.portal.dto.GoalDtos.*;
import com.atomquest.portal.entity.User;
import com.atomquest.portal.service.CurrentUserService;
import com.atomquest.portal.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {
    private final GoalService goalService;
    private final CurrentUserService currentUserService;

    @GetMapping
    List<GoalResponse> myGoals() {
        return goalService.myGoals(currentUserService.get());
    }

    @PostMapping
    GoalResponse create(@Valid @RequestBody GoalRequest request) {
        return goalService.create(currentUserService.get(), request);
    }

    @PatchMapping("/{id}")
    GoalResponse update(@PathVariable Long id, @Valid @RequestBody UpdateGoalRequest request) {
        return goalService.update(currentUserService.get(), id, request);
    }

    @GetMapping("/manager-comments")
    List<ManagerCommentResponse> managerComments() {
        return goalService.managerCommentsForEmployee(currentUserService.get());
    }

    @PostMapping("/submit")
    List<GoalResponse> submit() {
        return goalService.submit(currentUserService.get());
    }

    @PostMapping("/{id}/quarterly-updates")
    GoalResponse quarterly(@PathVariable Long id, @Valid @RequestBody QuarterUpdateRequest request) {
        return goalService.addQuarterlyUpdate(currentUserService.get(), id, request);
    }
}
