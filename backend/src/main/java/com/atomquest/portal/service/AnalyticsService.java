package com.atomquest.portal.service;

import com.atomquest.portal.entity.Goal;
import com.atomquest.portal.entity.GoalStatus;
import com.atomquest.portal.entity.Quarter;
import com.atomquest.portal.entity.QuarterlyUpdate;
import com.atomquest.portal.entity.Role;
import com.atomquest.portal.entity.User;
import com.atomquest.portal.repository.GoalRepository;
import com.atomquest.portal.repository.ManagerCommentRepository;
import com.atomquest.portal.repository.QuarterlyUpdateRepository;
import com.atomquest.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final GoalRepository goalRepository;
    private final QuarterlyUpdateRepository quarterlyUpdateRepository;
    private final UserRepository userRepository;
    private final ManagerCommentRepository managerCommentRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<Goal> goals = goalRepository.findAll();
        long total = goals.size();
        long completed = goals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();
        long approved = goals.stream().filter(g -> g.getStatus() == GoalStatus.APPROVED).count();
        List<QuarterlyUpdate> updates = quarterlyUpdateRepository.findAll();
        double avgProgress = updates.stream()
                .mapToDouble(QuarterlyUpdate::getProgressScore)
                .average()
                .orElse(0);

        Map<String, Long> byStatus = goals.stream()
                .collect(Collectors.groupingBy(g -> g.getStatus().name(), LinkedHashMap::new, Collectors.counting()));
        for (GoalStatus status : GoalStatus.values()) {
            byStatus.putIfAbsent(status.name(), 0L);
        }

        Map<String, Double> departmentProgress = goals.stream()
                .collect(Collectors.groupingBy(g -> g.getEmployee().getDepartment(), Collectors.averagingDouble(g -> {
                    if (g.getAchievement() == null || g.getTarget() == null || g.getTarget() == 0) return 0;
                    return Math.min(100, (g.getAchievement() / g.getTarget()) * 100);
                })));

        List<Map<String, Object>> trend = updates.stream()
                .collect(Collectors.groupingBy(q -> q.getQuarter().name(), Collectors.averagingDouble(QuarterlyUpdate::getProgressScore)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("quarter", e.getKey());
                    row.put("score", Math.round(e.getValue()));
                    return row;
                })
                .toList();

        List<Map<String, Object>> lowPerformers = goals.stream()
                .filter(g -> g.getAchievement() != null && g.getTarget() != null && g.getTarget() > 0)
                .map(g -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    double score = latestScore(updates, g);
                    row.put("id", g.getId());
                    row.put("title", g.getTitle());
                    row.put("employee", g.getEmployee().getName());
                    row.put("score", Math.round(score));
                    return row;
                })
                .filter(row -> ((Long) row.get("score")) < 50)
                .sorted(Comparator.comparing(row -> (Long) row.get("score")))
                .limit(5)
                .toList();

        Map<String, Long> thrustAreaDistribution = goals.stream()
                .filter(g -> g.getThrustArea() != null && !g.getThrustArea().isBlank())
                .collect(Collectors.groupingBy(Goal::getThrustArea, Collectors.counting()));

        Map<String, Long> uomDistribution = goals.stream()
                .collect(Collectors.groupingBy(g -> g.getUomType().name(), Collectors.counting()));

        Map<String, Double> managerEffectiveness = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MANAGER)
                .map(manager -> {
                    List<User> reports = userRepository.findByManagerId(manager.getId()).stream()
                            .filter(user -> user.getRole() == Role.EMPLOYEE)
                            .toList();
                    if (reports.isEmpty()) return Map.entry(manager.getName(), 0.0);
                    long completedCheckIns = reports.stream()
                            .filter(employee -> managerCommentRepository.existsByEmployeeIdAndManagerIdAndQuarter(employee.getId(), manager.getId(), activeQuarter()))
                            .count();
                    return Map.entry(manager.getName(), (completedCheckIns * 100.0) / reports.size());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Object> checkInCompletion = completionDashboard(userRepository.findAll(), activeQuarter());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGoals", total);
        result.put("completedGoals", completed);
        result.put("approvedGoals", approved);
        result.put("completionRate", total == 0 ? 0 : Math.round((completed * 100.0) / total));
        result.put("averageProgress", Math.round(avgProgress));
        result.put("employeeCount", userRepository.count());
        result.put("statusDistribution", byStatus);
        result.put("departmentProgress", departmentProgress);
        result.put("quarterlyTrend", trend);
        result.put("lowPerformingGoals", lowPerformers);
        result.put("thrustAreaDistribution", thrustAreaDistribution);
        result.put("uomDistribution", uomDistribution);
        result.put("managerEffectiveness", managerEffectiveness);
        result.put("checkInCompletion", checkInCompletion);

        return result;
    }

    private double latestScore(List<QuarterlyUpdate> updates, Goal goal) {
        return updates.stream()
                .filter(update -> update.getGoal().getId().equals(goal.getId()))
                .max(Comparator.comparing(QuarterlyUpdate::getUpdatedAt))
                .map(QuarterlyUpdate::getProgressScore)
                .orElseGet(() -> {
                    if (goal.getAchievement() == null || goal.getTarget() == null || goal.getTarget() == 0) return 0.0;
                    return Math.min(100, (goal.getAchievement() / goal.getTarget()) * 100);
                });
    }

    private Map<String, Object> completionDashboard(List<User> users, Quarter quarter) {
        List<User> employees = users.stream().filter(user -> user.getRole() == Role.EMPLOYEE).toList();
        List<Map<String, Object>> employeeRows = employees.stream().map(employee -> {
            boolean achievementDone = quarterlyUpdateRepository.existsByGoalEmployeeIdAndQuarter(employee.getId(), quarter);
            boolean managerDone = employee.getManager() != null
                    && managerCommentRepository.existsByEmployeeIdAndManagerIdAndQuarter(employee.getId(), employee.getManager().getId(), quarter);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId", employee.getId());
            row.put("employeeName", employee.getName());
            row.put("managerName", employee.getManager() == null ? "" : employee.getManager().getName());
            row.put("department", employee.getDepartment());
            row.put("achievementDone", achievementDone);
            row.put("managerCheckInDone", managerDone);
            return row;
        }).toList();
        long achievementDone = employeeRows.stream().filter(row -> Boolean.TRUE.equals(row.get("achievementDone"))).count();
        long managerDone = employeeRows.stream().filter(row -> Boolean.TRUE.equals(row.get("managerCheckInDone"))).count();
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("quarter", quarter.name());
        dashboard.put("employeeRows", employeeRows);
        dashboard.put("achievementCompletionRate", employees.isEmpty() ? 0 : Math.round((achievementDone * 100.0) / employees.size()));
        dashboard.put("managerCheckInCompletionRate", employees.isEmpty() ? 0 : Math.round((managerDone * 100.0) / employees.size()));
        return dashboard;
    }

    private Quarter activeQuarter() {
        int month = java.time.LocalDate.now().getMonthValue();
        if (month == 7) return Quarter.Q1;
        if (month == 10) return Quarter.Q2;
        if (month == 1) return Quarter.Q3;
        if (month == 3 || month == 4) return Quarter.Q4;
        return Quarter.Q1;
    }
}
