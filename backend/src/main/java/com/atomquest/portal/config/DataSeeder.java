package com.atomquest.portal.config;

import com.atomquest.portal.entity.*;
import com.atomquest.portal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final SharedGoalRepository sharedGoalRepository;
    private final QuarterlyUpdateRepository quarterlyUpdateRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDemoData() {
        return args -> {
            if (userRepository.existsByEmail("admin@demo.com")) {
                return;
            }

            User admin = userRepository.save(User.builder()
                    .name("Ananya HR")
                    .email("admin@demo.com")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.ADMIN)
                    .department("People Success")
                    .build());
            User manager = userRepository.save(User.builder()
                    .name("Rahul Manager")
                    .email("manager@demo.com")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.MANAGER)
                    .manager(admin)
                    .department("Engineering")
                    .build());
            User employee = userRepository.save(User.builder()
                    .name("Vikas Employee")
                    .email("employee@demo.com")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.EMPLOYEE)
                    .manager(manager)
                    .department("Engineering")
                    .build());

            SharedGoal shared = sharedGoalRepository.save(SharedGoal.builder()
                    .title("Improve platform reliability score")
                    .description("Improve reliability through preventive release checks and faster incident response.")
                    .thrustArea("Operational Excellence")
                    .uomType(UomType.PERCENT)
                    .target(99.5)
                    .deadline(LocalDate.now().plusMonths(3))
                    .owner(admin)
                    .department("Engineering")
                    .build());

            Goal g1 = goalRepository.save(Goal.builder()
                    .employee(employee)
                    .title("Deliver customer-facing goal portal MVP")
                    .description("Ship full employee, manager, and HR goal workflows with analytics.")
                    .thrustArea("Product Delivery")
                    .uomType(UomType.PERCENT)
                    .target(100.0)
                    .achievement(72.0)
                    .weightage(35)
                    .status(GoalStatus.ON_TRACK)
                    .locked(true)
                    .deadline(LocalDate.now().plusMonths(2))
                    .build());
            Goal g2 = goalRepository.save(Goal.builder()
                    .employee(employee)
                    .title(shared.getTitle())
                    .description("Reduce avoidable incidents through better release checks.")
                    .thrustArea("Operational Excellence")
                    .uomType(UomType.PERCENT)
                    .target(shared.getTarget())
                    .achievement(98.7)
                    .weightage(25)
                    .status(GoalStatus.APPROVED)
                    .locked(true)
                    .sharedGoal(shared)
                    .deadline(LocalDate.now().plusMonths(3))
                    .build());
            goalRepository.save(Goal.builder()
                    .employee(employee)
                    .title("Automate quarterly reporting")
                    .description("Generate manager-ready CSV and analytics snapshots.")
                    .thrustArea("Efficiency")
                    .uomType(UomType.NUMERIC)
                    .target(12.0)
                    .achievement(5.0)
                    .weightage(20)
                    .status(GoalStatus.SUBMITTED)
                    .locked(false)
                    .deadline(LocalDate.now().plusMonths(1))
                    .build());
            goalRepository.save(Goal.builder()
                    .employee(employee)
                    .title("Mentor junior engineers")
                    .description("Run structured learning sessions and code review clinics.")
                    .thrustArea("People Development")
                    .uomType(UomType.NUMERIC)
                    .target(8.0)
                    .achievement(3.0)
                    .weightage(20)
                    .status(GoalStatus.ON_TRACK)
                    .locked(true)
                    .deadline(LocalDate.now().plusMonths(4))
                    .build());

            quarterlyUpdateRepository.save(QuarterlyUpdate.builder()
                    .goal(g1)
                    .quarter(Quarter.Q1)
                    .achievement(40.0)
                    .comment("Architecture and login flow completed.")
                    .progressScore(40.0)
                    .build());
            quarterlyUpdateRepository.save(QuarterlyUpdate.builder()
                    .goal(g1)
                    .quarter(Quarter.Q2)
                    .achievement(72.0)
                    .comment("Dashboards and analytics are demo-ready.")
                    .progressScore(72.0)
                    .build());
            quarterlyUpdateRepository.save(QuarterlyUpdate.builder()
                    .goal(g2)
                    .quarter(Quarter.Q2)
                    .achievement(98.7)
                    .comment("Reliability trend is healthy.")
                    .progressScore(99.0)
                    .build());

            auditLogRepository.save(AuditLog.builder()
                    .user(admin)
                    .action("SEED_DEMO_DATA")
                    .entityType("System")
                    .newValue("Demo users, goals, shared goals, and updates created")
                    .build());
        };
    }
}
