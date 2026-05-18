package com.atomquest.portal.service;

import com.atomquest.portal.entity.Quarter;
import com.atomquest.portal.entity.SystemConfig;
import com.atomquest.portal.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CycleService {
    private final SystemConfigRepository configRepository;

    public boolean isGoalSettingWindowOpen() {
        if (isOverrideActive("GOAL_SETTING_WINDOW_OVERRIDE")) return true;
        int month = LocalDate.now().getMonthValue();
        return month == 5; // May
    }

    public boolean isCheckInWindowOpen(Quarter quarter) {
        if (isOverrideActive("CHECKIN_WINDOW_OVERRIDE_" + quarter.name())) return true;
        int month = LocalDate.now().getMonthValue();
        return switch (quarter) {
            case Q1 -> month == 7; // July
            case Q2 -> month == 10; // October
            case Q3 -> month == 1; // January
            case Q4 -> month == 3 || month == 4; // March/April
        };
    }

    public boolean isAnyCheckInWindowOpen() {
        return isCheckInWindowOpen(Quarter.Q1) || isCheckInWindowOpen(Quarter.Q2) ||
               isCheckInWindowOpen(Quarter.Q3) || isCheckInWindowOpen(Quarter.Q4);
    }

    private boolean isOverrideActive(String key) {
        return configRepository.findById(key)
                .map(SystemConfig::getValue)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    public void setOverride(String key, boolean value) {
        SystemConfig config = configRepository.findById(key)
                .orElse(SystemConfig.builder().key(key).build());
        config.setValue(String.valueOf(value));
        configRepository.save(config);
    }
    
    public boolean getOverride(String key) {
        return isOverrideActive(key);
    }
}
