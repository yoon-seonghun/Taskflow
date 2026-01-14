package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.domain.EmailLog;
import com.taskflow.dto.email.EmailSendRequest;
import com.taskflow.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 이메일 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    /**
     * 수동 메일 발송
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<List<EmailLog>>> sendEmail(
            @Valid @RequestBody EmailSendRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<EmailLog> results = emailService.sendEmail(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(results, "메일 발송이 요청되었습니다."));
    }

    /**
     * 메일 발송 이력 조회
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<EmailLog> logs = emailService.getEmailLogs(page, size);
        int total = emailService.getTotalEmailLogCount();

        Map<String, Object> result = new HashMap<>();
        result.put("content", logs);
        result.put("page", page);
        result.put("size", size);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 상태별 메일 이력 조회
     */
    @GetMapping("/logs/status/{status}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmailLogsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<EmailLog> logs = emailService.getEmailLogsByStatus(status.toUpperCase(), page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("content", logs);
        result.put("page", page);
        result.put("size", size);
        result.put("status", status.toUpperCase());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 메일 재발송
     */
    @PostMapping("/logs/{emailLogId}/resend")
    public ResponseEntity<ApiResponse<EmailLog>> resendEmail(
            @PathVariable Long emailLogId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            EmailLog result = emailService.resendEmail(emailLogId, userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success(result, "메일 재발송이 요청되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 업무 담당자 지정 알림 발송 (내부 호출용)
     */
    @PostMapping("/notify/task-assign")
    public ResponseEntity<ApiResponse<EmailLog>> notifyTaskAssign(
            @RequestParam Long itemId,
            @RequestParam String assigneeUsername,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EmailLog result = emailService.sendTaskAssignNotification(itemId, assigneeUsername, userDetails.getUsername());
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error("알림을 발송할 수 없습니다. 수신자 이메일을 확인해주세요."));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 업무 이관 알림 발송 (내부 호출용)
     */
    @PostMapping("/notify/task-transfer")
    public ResponseEntity<ApiResponse<EmailLog>> notifyTaskTransfer(
            @RequestParam Long itemId,
            @RequestParam String targetUsername,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EmailLog result = emailService.sendTaskTransferNotification(itemId, targetUsername, userDetails.getUsername());
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error("알림을 발송할 수 없습니다. 수신자 이메일을 확인해주세요."));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 업무 공유 알림 발송 (내부 호출용)
     */
    @PostMapping("/notify/task-share")
    public ResponseEntity<ApiResponse<EmailLog>> notifyTaskShare(
            @RequestParam Long itemId,
            @RequestParam String targetUsername,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EmailLog result = emailService.sendTaskShareNotification(itemId, targetUsername, userDetails.getUsername());
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error("알림을 발송할 수 없습니다. 수신자 이메일을 확인해주세요."));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
