package com.atomquest.portal.dto;

import com.atomquest.portal.entity.GoalStatus;
import com.atomquest.portal.entity.ProgressStatus;
import com.atomquest.portal.entity.Quarter;
import com.atomquest.portal.entity.UomType;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;

public class GoalDtos {
    public record GoalRequest(@NotBlank String title, String description, @NotBlank String thrustArea,
                              @NotNull UomType uomType, @NotNull @Positive Double target,
                              @NotNull @Min(10) @Max(100) Integer weightage,
                              @NotNull LocalDate deadline, Long sharedGoalId) {}

    public record GoalResponse(Long id, Long employeeId, String employeeName, String title, String description,
                               String thrustArea, UomType uomType, Double target, Double achievement,
                               Integer weightage, GoalStatus status, boolean locked, Long sharedGoalId,
                               LocalDate deadline, Instant createdAt, Quarter latestQuarter,
                               Double latestProgressScore, ProgressStatus latestProgressStatus,
                               String latestUpdateComment, LocalDate latestCompletionDate) {}

    public record UpdateGoalRequest(String title, String description, String thrustArea, UomType uomType,
                                    Double target, @Min(10) @Max(100) Integer weightage, GoalStatus status,
                                    LocalDate deadline, Long sharedGoalId) {}

    public record ReviewGoalRequest(@NotNull GoalStatus decision, String managerNote, Double target,
                                    @Min(10) @Max(100) Integer weightage) {}

    public record ManagerCommentResponse(Long id, Long employeeId, String employeeName, String managerName,
                                         String quarter, String comment, Instant createdAt) {}

    public record QuarterUpdateRequest(@NotNull Quarter quarter, @NotNull @PositiveOrZero Double achievement,
                                       String comment, GoalStatus status, ProgressStatus progressStatus,
                                       LocalDate completionDate) {}
}
