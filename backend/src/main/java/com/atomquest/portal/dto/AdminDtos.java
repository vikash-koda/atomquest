package com.atomquest.portal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.atomquest.portal.entity.UomType;

import java.time.LocalDate;
import java.util.List;

public class AdminDtos {
    public record SharedGoalRequest(@NotBlank String title, String description, @NotBlank String thrustArea,
                                    @NotNull UomType uomType, @NotNull @Positive Double target,
                                    @NotNull Long ownerId, @NotBlank String department, @NotNull LocalDate deadline) {}

    public record PushSharedGoalRequest(@NotBlank String title, String description, @NotBlank String thrustArea,
                                        @NotNull UomType uomType, @NotNull @Positive Double target,
                                        @NotNull Long ownerId, @NotBlank String department,
                                        @NotNull @Min(10) @Max(100) Integer defaultWeightage, @NotNull LocalDate deadline,
                                        List<Long> employeeIds) {}
}
