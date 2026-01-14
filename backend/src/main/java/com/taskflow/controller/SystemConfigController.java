package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.domain.SystemConfig;
import com.taskflow.dto.config.SmtpConfigResponse;
import com.taskflow.dto.config.SmtpTestRequest;
import com.taskflow.dto.config.SystemConfigUpdateRequest;
import com.taskflow.service.EmailService;
import com.taskflow.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 시스템 설정 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final EmailService emailService;

    /**
     * SMTP 설정 조회
     */
    @GetMapping("/smtp")
    public ResponseEntity<ApiResponse<SmtpConfigResponse>> getSmtpConfig() {
        SmtpConfigResponse response = systemConfigService.getConfigsByGroup(SystemConfig.GROUP_SMTP);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * SMTP 설정 업데이트
     */
    @PutMapping("/smtp")
    public ResponseEntity<ApiResponse<Void>> updateSmtpConfig(
            @Valid @RequestBody SystemConfigUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        systemConfigService.updateConfigs(SystemConfig.GROUP_SMTP, request, userDetails.getUsername());
        log.info("SMTP config updated by {}", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "SMTP 설정이 업데이트되었습니다."));
    }

    /**
     * SMTP 비밀번호 업데이트
     */
    @PutMapping("/smtp/password")
    public ResponseEntity<ApiResponse<Void>> updateSmtpPassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("비밀번호가 필요합니다."));
        }

        systemConfigService.updatePasswordConfig(
                SystemConfig.GROUP_SMTP,
                SystemConfig.SMTP_PASSWORD,
                password,
                userDetails.getUsername()
        );
        log.info("SMTP password updated by {}", userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "SMTP 비밀번호가 업데이트되었습니다."));
    }

    /**
     * SMTP 연결 테스트
     */
    @PostMapping("/smtp/test-connection")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testSmtpConnection() {
        boolean success = emailService.testSmtpConnection();
        Map<String, Object> result = Map.of(
                "success", success,
                "message", success ? "SMTP 연결 성공" : "SMTP 연결 실패"
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 테스트 메일 발송
     */
    @PostMapping("/smtp/test-send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestEmail(
            @Valid @RequestBody SmtpTestRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            var emailLog = emailService.sendTestEmail(request.getTestEmail(), userDetails.getUsername());
            boolean success = "SENT".equals(emailLog.getStatus());

            Map<String, Object> result = Map.of(
                    "success", success,
                    "emailLogId", emailLog.getEmailLogId(),
                    "status", emailLog.getStatus(),
                    "message", success ? "테스트 메일 발송 성공" : "테스트 메일 발송 실패: " + emailLog.getErrorMessage()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Test email send failed", e);
            return ResponseEntity.ok(ApiResponse.error("테스트 메일 발송 실패: " + e.getMessage()));
        }
    }

    /**
     * 시스템 설정 조회 (그룹별)
     */
    @GetMapping("/{group}")
    public ResponseEntity<ApiResponse<SmtpConfigResponse>> getConfigByGroup(
            @PathVariable String group
    ) {
        SmtpConfigResponse response = systemConfigService.getConfigsByGroup(group.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 시스템 설정 업데이트 (그룹별)
     */
    @PutMapping("/{group}")
    public ResponseEntity<ApiResponse<Void>> updateConfigByGroup(
            @PathVariable String group,
            @Valid @RequestBody SystemConfigUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        systemConfigService.updateConfigs(group.toUpperCase(), request, userDetails.getUsername());
        log.info("{} config updated by {}", group, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "설정이 업데이트되었습니다."));
    }
}
