package com.atomquest.portal.service;

import com.atomquest.portal.dto.GoalDtos.*;
import com.atomquest.portal.entity.*;
import com.atomquest.portal.exception.ApiException;
import com.atomquest.portal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {
    private final GoalRepository goalRepository;
    private final SharedGoalRepository sharedGoalRepository;
    private final QuarterlyUpdateRepository quarterlyUpdateRepository;
    private final ManagerCommentRepository managerCommentRepository;
    private final AuditService auditService;
    private final CycleService cycleService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GoalResponse> myGoals(User user) {
        return goalRepository.findByEmployeeIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> teamGoals(User manager) {
        return goalRepository.findByEmployeeManagerIdOrderByCreatedAtDesc(manager.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> allGoals() {
        return goalRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ManagerCommentResponse> managerCommentsForEmployee(User employee) {
        return managerCommentRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .map(comment -> new ManagerCommentResponse(
                        comment.getId(),
                        comment.getEmployee().getId(),
                        comment.getEmployee().getName(),
                        comment.getManager().getName(),
                        comment.getQuarter().name(),
                        comment.getComment(),
                        comment.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagerCommentResponse> managerCommentsByManager(User manager) {
        return managerCommentRepository.findByManagerIdOrderByCreatedAtDesc(manager.getId()).stream()
                .map(comment -> new ManagerCommentResponse(
                        comment.getId(),
                        comment.getEmployee().getId(),
                        comment.getEmployee().getName(),
                        comment.getManager().getName(),
                        comment.getQuarter().name(),
                        comment.getComment(),
                        comment.getCreatedAt()))
                .toList();
    }

    @Transactional
    public GoalResponse create(User employee, GoalRequest request) {
        if (!cycleService.isGoalSettingWindowOpen()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal creation is only allowed during the Goal Setting window");
        }
        if (goalRepository.countByEmployeeId(employee.getId()) >= GoalRules.MAX_GOALS_PER_EMPLOYEE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Maximum " + GoalRules.MAX_GOALS_PER_EMPLOYEE + " goals allowed per employee");
        }
        GoalRules.assertWeightageInRange(request.weightage());
        SharedGoal sharedGoal = null;
        String title = request.title();
        Double target = request.target();
        if (request.sharedGoalId() != null) {
            sharedGoal = sharedGoalRepository.findById(request.sharedGoalId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Shared goal not found"));
            if (!sharedGoal.getDepartment().equalsIgnoreCase(employee.getDepartment())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Shared goal is not available for your department");
            }
            title = sharedGoal.getTitle();
            target = sharedGoal.getTarget();
            request = new GoalRequest(
                    title,
                    sharedGoal.getDescription() == null ? request.description() : sharedGoal.getDescription(),
                    sharedGoal.getThrustArea() == null ? request.thrustArea() : sharedGoal.getThrustArea(),
                    sharedGoal.getUomType() == null ? request.uomType() : sharedGoal.getUomType(),
                    target,
                    request.weightage(),
                    sharedGoal.getDeadline() == null ? request.deadline() : sharedGoal.getDeadline(),
                    request.sharedGoalId());
        }
        Goal goal = goalRepository.save(Goal.builder()
                .employee(employee)
                .title(title)
                .description(request.description())
                .thrustArea(request.thrustArea())
                .uomType(request.uomType())
                .target(target)
                .weightage(request.weightage())
                .status(GoalStatus.DRAFT)
                .locked(false)
                .sharedGoal(sharedGoal)
                .deadline(request.deadline())
                .build());
        auditService.log(employee, "CREATE_GOAL", "Goal", goal.getId(), null, goal.getTitle());
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse update(User actor, Long id, UpdateGoalRequest request) {
        Goal goal = findOwnedOrTeam(actor, id);
        if (goal.isLocked()) {
            throw new ApiException(HttpStatus.LOCKED, "Goal is locked. Ask Admin/HR to unlock it.");
        }
        if (request.status() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal status can only be changed through submit or review workflows");
        }
        boolean isOwner = goal.getEmployee().getId().equals(actor.getId());
        boolean isManager = actor.getRole() == Role.MANAGER
                && goal.getEmployee().getManager() != null
                && goal.getEmployee().getManager().getId().equals(actor.getId());
        boolean isAdmin = actor.getRole() == Role.ADMIN;

        if (isOwner && actor.getRole() == Role.EMPLOYEE) {
            if (!GoalRules.isEditableByEmployee(goal)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft or rejected goals can be edited by employee");
            }
        } else if (isManager && !isOwner) {
            if (goal.getStatus() != GoalStatus.SUBMITTED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Managers can edit only submitted goals during review");
            }
            if (hasNonReviewFields(request)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Managers can edit only target and weightage during review");
            }
        } else if (isAdmin && !isOwner) {
            if (goal.getStatus() != GoalStatus.SUBMITTED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Admins can edit only submitted goals during review");
            }
            if (hasNonReviewFields(request)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Admins can edit only target and weightage during review");
            }
        }

        if (goal.getSharedGoal() != null && isOwner && actor.getRole() == Role.EMPLOYEE) {
            if (request.title() != null || request.target() != null || request.description() != null
                    || request.thrustArea() != null || request.uomType() != null || request.deadline() != null
                    || request.sharedGoalId() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Recipients can edit only weightage for shared goals");
            }
        }
        String oldValue = goalSnapshot(goal);
        if (goal.getSharedGoal() == null && request.sharedGoalId() != null) {
            SharedGoal sharedGoal = sharedGoalRepository.findById(request.sharedGoalId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Shared goal not found"));
            goal.setSharedGoal(sharedGoal);
            goal.setTitle(sharedGoal.getTitle());
            if (sharedGoal.getDescription() != null) goal.setDescription(sharedGoal.getDescription());
            if (sharedGoal.getThrustArea() != null) goal.setThrustArea(sharedGoal.getThrustArea());
            if (sharedGoal.getUomType() != null) goal.setUomType(sharedGoal.getUomType());
            goal.setTarget(sharedGoal.getTarget());
            if (sharedGoal.getDeadline() != null) goal.setDeadline(sharedGoal.getDeadline());
        }
        if (request.title() != null) goal.setTitle(request.title());
        if (request.description() != null) goal.setDescription(request.description());
        if (request.thrustArea() != null) goal.setThrustArea(request.thrustArea());
        if (request.uomType() != null) goal.setUomType(request.uomType());
        if (request.target() != null) goal.setTarget(request.target());
        if (request.weightage() != null) {
            GoalRules.assertWeightageInRange(request.weightage());
            goal.setWeightage(request.weightage());
        }
        if (request.deadline() != null) goal.setDeadline(request.deadline());
        auditService.log(actor, "UPDATE_GOAL", "Goal", goal.getId(), oldValue, goalSnapshot(goal));
        return toResponse(goal);
    }

    @Transactional
    public List<GoalResponse> submit(User employee) {
        if (!cycleService.isGoalSettingWindowOpen()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal submission is only allowed during the Goal Setting window");
        }
        List<Goal> goals = goalRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId());
        if (goals.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Create at least one goal before submission");
        }
        if (goals.stream().anyMatch(goal -> goal.getStatus() == GoalStatus.SUBMITTED)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goals are already pending manager approval");
        }
        List<Goal> toSubmit = goals.stream()
                .filter(goal -> goal.getStatus() == GoalStatus.DRAFT || goal.getStatus() == GoalStatus.REJECTED)
                .toList();
        if (toSubmit.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No draft or rejected goals available to submit");
        }
        GoalRules.assertTotalWeightageEquals100(goals);
        toSubmit.forEach(goal -> goal.setStatus(GoalStatus.SUBMITTED));
        auditService.log(employee, "SUBMIT_GOAL_SHEET", "Goal", employee.getId(), null, "totalWeightage=100");
        return goalRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GoalResponse review(User manager, Long id, ReviewGoalRequest request) {
        Goal goal = findOwnedOrTeam(manager, id);
        if (goal.getStatus() != GoalStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only submitted goals can be reviewed");
        }
        if (request.decision() != GoalStatus.APPROVED && request.decision() != GoalStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }
        if (goal.getSharedGoal() != null && request.target() != null
                && !request.target().equals(goal.getSharedGoal().getTarget())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Shared goal target is read-only; adjust weightage only");
        }
        String oldValue = goalSnapshot(goal);
        if (request.target() != null) goal.setTarget(request.target());
        if (request.weightage() != null) {
            GoalRules.assertWeightageInRange(request.weightage());
            goal.setWeightage(request.weightage());
        }
        if (request.decision() == GoalStatus.APPROVED) {
            List<Goal> employeeGoals = goalRepository.findByEmployeeIdOrderByCreatedAtDesc(goal.getEmployee().getId());
            GoalRules.assertTotalWeightageEquals100(employeeGoals);
        }
        goal.setStatus(request.decision());
        goal.setLocked(request.decision() == GoalStatus.APPROVED);
        if (request.managerNote() != null && !request.managerNote().isBlank()) {
            managerCommentRepository.save(ManagerComment.builder()
                    .manager(manager)
                    .employee(goal.getEmployee())
                    .quarter(Quarter.Q1)
                    .comment(request.managerNote().trim())
                    .build());
        }
        auditService.log(manager, "REVIEW_GOAL_" + request.decision(), "Goal", goal.getId(), oldValue, goalSnapshot(goal));
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse unlock(User admin, Long id) {
        Goal goal = goalRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found"));
        String oldValue = goalSnapshot(goal);
        goal.setLocked(false);
        goal.setStatus(GoalStatus.DRAFT);
        auditService.log(admin, "UNLOCK_GOAL", "Goal", id, oldValue, goalSnapshot(goal));
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse addQuarterlyUpdate(User employee, Long goalId, QuarterUpdateRequest request) {
        if (!cycleService.isCheckInWindowOpen(request.quarter())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quarterly updates for this quarter are allowed only during active check-in period");
        }
        Goal goal = goalRepository.findById(goalId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found"));
        if (!goal.getEmployee().getId().equals(employee.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot update another employee's goal");
        }
        if (!goal.isLocked() && goal.getStatus() != GoalStatus.APPROVED
                && goal.getStatus() != GoalStatus.NOT_STARTED
                && goal.getStatus() != GoalStatus.ON_TRACK
                && goal.getStatus() != GoalStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quarterly updates are allowed only after goal approval");
        }
        if (goal.getSharedGoal() != null && goal.getSharedGoal().getOwner().getId().equals(employee.getId())) {
            for (Goal linked : goalRepository.findBySharedGoalId(goal.getSharedGoal().getId())) {
                applyQuarterlyUpdate(linked, request);
            }
            auditService.log(employee, "SYNC_SHARED_GOAL_ACHIEVEMENT", "SharedGoal", goal.getSharedGoal().getId(), null, request.quarter().name());
        } else {
            applyQuarterlyUpdate(goal, request);
        }
        auditService.log(employee, "QUARTERLY_UPDATE", "QuarterlyUpdate", goalId, null, request.quarter().name());
        return toResponse(goal);
    }

    @Transactional
    public ManagerCommentResponse addManagerComment(User manager, Long employeeId, Quarter quarter, String commentText) {
        User employee = userRepository.findById(employeeId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (employee.getManager() == null || !employee.getManager().getId().equals(manager.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to add comments for this employee");
        }
        if (!cycleService.isCheckInWindowOpen(quarter)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Check-in window is not open for this quarter");
        }
        ManagerComment comment = managerCommentRepository.save(ManagerComment.builder()
                .manager(manager)
                .employee(employee)
                .quarter(quarter)
                .comment(commentText)
                .build());
        auditService.log(manager, "MANAGER_CHECK_IN_COMMENT", "User", employee.getId(), null, quarter.name());
        return new ManagerCommentResponse(comment.getId(), employee.getId(), employee.getName(), manager.getName(), quarter.name(), comment.getComment(), comment.getCreatedAt());
    }

    private boolean hasNonReviewFields(UpdateGoalRequest request) {
        return request.title() != null || request.description() != null || request.thrustArea() != null
                || request.uomType() != null || request.deadline() != null || request.sharedGoalId() != null;
    }

    private void applyQuarterlyUpdate(Goal goal, QuarterUpdateRequest request) {
        double score = progressScore(goal, request.achievement(), request.completionDate());
        goal.setAchievement(request.achievement());
        if (request.progressStatus() != null) {
            goal.setStatus(GoalStatus.valueOf(request.progressStatus().name()));
        } else if (request.status() != null) {
            if (request.status() != GoalStatus.NOT_STARTED && request.status() != GoalStatus.ON_TRACK && request.status() != GoalStatus.COMPLETED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Progress status must be Not Started, On Track, or Completed");
            }
            goal.setStatus(request.status());
        }
        QuarterlyUpdate update = quarterlyUpdateRepository.findByGoalIdAndQuarter(goal.getId(), request.quarter())
                .orElse(QuarterlyUpdate.builder().goal(goal).quarter(request.quarter()).build());
        update.setAchievement(request.achievement());
        update.setComment(request.comment());
        update.setProgressScore(score);
        update.setStatus(request.progressStatus());
        update.setCompletionDate(request.completionDate());
        quarterlyUpdateRepository.save(update);
    }

    private double progressScore(Goal goal, double achievement, LocalDate completionDate) {
        if (achievement < 0) achievement = 0;
        if (goal.getTarget() == null || goal.getTarget() == 0) {
            return achievement == 0 ? 100 : 0;
        }
        return switch (goal.getUomType()) {
            case NUMERIC, PERCENT -> clamp((achievement / goal.getTarget()) * 100);
            case MAX -> clamp((goal.getTarget() / Math.max(achievement, 0.0001)) * 100);
            case ZERO -> achievement == 0 ? 100 : 0;
            case TIMELINE -> {
                if (completionDate != null) {
                    yield completionDate.isAfter(goal.getDeadline()) ? 0 : 100;
                }
                yield clamp((achievement / goal.getTarget()) * 100);
            }
        };
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private String goalSnapshot(Goal goal) {
        return "title=" + goal.getTitle()
                + ",target=" + goal.getTarget()
                + ",weightage=" + goal.getWeightage()
                + ",status=" + goal.getStatus()
                + ",locked=" + goal.isLocked()
                + ",deadline=" + goal.getDeadline();
    }

    private Goal findOwnedOrTeam(User actor, Long id) {
        Goal goal = goalRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found"));
        boolean own = goal.getEmployee().getId().equals(actor.getId());
        boolean manager = goal.getEmployee().getManager() != null && goal.getEmployee().getManager().getId().equals(actor.getId());
        boolean admin = actor.getRole() == Role.ADMIN;
        if (!own && !manager && !admin) throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed for this goal");
        return goal;
    }

    public GoalResponse toResponse(Goal goal) {
        QuarterlyUpdate latestUpdate = goal.getId() == null ? null
                : quarterlyUpdateRepository.findTopByGoalIdOrderByUpdatedAtDesc(goal.getId()).orElse(null);
        return new GoalResponse(goal.getId(), goal.getEmployee().getId(), goal.getEmployee().getName(), goal.getTitle(),
                goal.getDescription(), goal.getThrustArea(), goal.getUomType(), goal.getTarget(), goal.getAchievement(),
                goal.getWeightage(), goal.getStatus(), goal.isLocked(),
                goal.getSharedGoal() == null ? null : goal.getSharedGoal().getId(), goal.getDeadline(), goal.getCreatedAt(),
                latestUpdate == null ? null : latestUpdate.getQuarter(),
                latestUpdate == null ? progressScore(goal, goal.getAchievement() == null ? 0 : goal.getAchievement(), null) : latestUpdate.getProgressScore(),
                latestUpdate == null ? null : latestUpdate.getStatus(),
                latestUpdate == null ? null : latestUpdate.getComment(),
                latestUpdate == null ? null : latestUpdate.getCompletionDate());
    }
}
