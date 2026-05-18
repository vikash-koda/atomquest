package com.atomquest.portal.controller;

import com.atomquest.portal.entity.SharedGoal;
import com.atomquest.portal.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shared-goals")
@RequiredArgsConstructor
public class SharedGoalController {
    private final AdminService adminService;

    @GetMapping
    List<SharedGoal> sharedGoals(@RequestParam(required = false) String department) {
        return adminService.sharedGoals(department, false);
    }
}
