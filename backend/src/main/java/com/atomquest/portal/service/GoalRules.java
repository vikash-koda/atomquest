package com.atomquest.portal.service;

import com.atomquest.portal.entity.Goal;
import com.atomquest.portal.entity.GoalStatus;
import com.atomquest.portal.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.List;

public final class GoalRules {
    public static final int MAX_GOALS_PER_EMPLOYEE = 8;
    public static final int MIN_WEIGHTAGE = 10;
    public static final int MAX_WEIGHTAGE = 100;
    public static final int REQUIRED_TOTAL_WEIGHTAGE = 100;

    private GoalRules() {}

    public static void assertWeightageInRange(int weightage) {
        if (weightage < MIN_WEIGHTAGE || weightage > MAX_WEIGHTAGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Weightage must be between " + MIN_WEIGHTAGE + "% and " + MAX_WEIGHTAGE + "%");
        }
    }

    public static int totalWeightage(List<Goal> goals) {
        return goals.stream().mapToInt(Goal::getWeightage).sum();
    }

    public static void assertTotalWeightageEquals100(List<Goal> goals) {
        int total = totalWeightage(goals);
        if (total != REQUIRED_TOTAL_WEIGHTAGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Total weightage must equal " + REQUIRED_TOTAL_WEIGHTAGE + "% (current: " + total + "%)");
        }
    }

    public static boolean isEditableByEmployee(Goal goal) {
        return !goal.isLocked()
                && (goal.getStatus() == GoalStatus.DRAFT || goal.getStatus() == GoalStatus.REJECTED);
    }
}
