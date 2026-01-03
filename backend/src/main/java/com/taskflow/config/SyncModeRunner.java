package com.taskflow.config;

import com.taskflow.dto.sync.SyncResult;
import com.taskflow.service.DepartmentSyncService;
import com.taskflow.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Sync Mode Runner
 *
 * --sync 옵션으로 실행 시 전체 동기화 수행 후 종료
 *
 * 사용법:
 *   java -jar taskflow.jar --sync
 *
 * Cron 예시:
 *   0 2 * * * /usr/bin/java -jar /app/taskflow.jar --sync >> /var/log/taskflow-sync.log 2>&1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "external"
)
public class SyncModeRunner implements CommandLineRunner {

    private final UserSyncService userSyncService;
    private final DepartmentSyncService departmentSyncService;
    private final UserManagementProperties userManagementProperties;

    @Override
    public void run(String... args) throws Exception {
        // --sync 옵션이 없으면 실행하지 않음 (웹 모드)
        if (!Arrays.asList(args).contains("--sync")) {
            log.debug("SyncModeRunner skipped - not in sync mode");
            return;
        }

        // External 모드가 아니면 경고 후 종료
        if (!userManagementProperties.isExternalMode()) {
            log.warn("Sync mode requires external user management mode. Current mode: internal");
            log.warn("Set taskflow.user-management.mode=external to enable sync.");
            System.exit(1);
            return;
        }

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║            TaskFlow External DB Synchronization              ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        boolean success = true;

        try {
            // 1. 부서 동기화 (선행 필수)
            log.info("");
            log.info("▶ Step 1: Department Synchronization");
            log.info("─────────────────────────────────────────────────────────────────");

            SyncResult deptResult = departmentSyncService.syncAllDepartments();
            printSyncResult("Department", deptResult);

            if (!deptResult.isSuccess()) {
                log.error("Department sync failed. Aborting user sync.");
                success = false;
            } else {
                // 2. 사용자 동기화
                log.info("");
                log.info("▶ Step 2: User Synchronization");
                log.info("─────────────────────────────────────────────────────────────────");

                SyncResult userResult = userSyncService.syncAllUsers();
                printSyncResult("User", userResult);

                if (!userResult.isSuccess()) {
                    success = false;
                }
            }

            // 결과 요약
            log.info("");
            log.info("═══════════════════════════════════════════════════════════════");
            if (success) {
                log.info("✓ Synchronization completed successfully");
            } else {
                log.error("✗ Synchronization completed with errors");
            }
            log.info("═══════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("Synchronization failed with exception", e);
            success = false;
        }

        // CLI 모드이므로 종료
        System.exit(success ? 0 : 1);
    }

    /**
     * 동기화 결과 출력
     */
    private void printSyncResult(String type, SyncResult result) {
        if (result.isSuccess()) {
            log.info("  {} sync completed:", type);
            log.info("    - Created:     {}", result.getCreated());
            log.info("    - Updated:     {}", result.getUpdated());
            log.info("    - Deactivated: {}", result.getDeactivated());
            log.info("    - Skipped:     {}", result.getSkipped());
            log.info("    - Total:       {}", result.getTotalProcessed());

            if (result.getStartedAt() != null && result.getCompletedAt() != null) {
                long durationMs = java.time.Duration.between(
                        result.getStartedAt(), result.getCompletedAt()).toMillis();
                log.info("    - Duration:    {} ms", durationMs);
            }
        } else {
            log.error("  {} sync FAILED: {}", type, result.getErrorMessage());
        }
    }
}
