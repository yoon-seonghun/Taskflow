package com.taskflow.service;

import com.taskflow.domain.EmailLog;
import com.taskflow.dto.email.EmailSendRequest;

import java.util.List;

/**
 * 이메일 서비스 인터페이스
 */
public interface EmailService {

    /**
     * 수동 메일 발송
     */
    List<EmailLog> sendEmail(EmailSendRequest request, String senderUsername);

    /**
     * 업무 담당자 지정 알림 메일
     */
    EmailLog sendTaskAssignNotification(Long itemId, String assigneeUsername, String senderUsername);

    /**
     * 업무 이관 알림 메일
     */
    EmailLog sendTaskTransferNotification(Long itemId, String targetUsername, String senderUsername);

    /**
     * 업무 공유 알림 메일
     */
    EmailLog sendTaskShareNotification(Long itemId, String targetUsername, String senderUsername);

    /**
     * 마감일 알림 메일 (배치용)
     */
    List<EmailLog> sendDueDateAlerts();

    /**
     * SMTP 연결 테스트
     */
    boolean testSmtpConnection();

    /**
     * 테스트 메일 발송
     */
    EmailLog sendTestEmail(String recipientEmail, String senderUsername);

    /**
     * 메일 발송 이력 조회 (페이징)
     */
    List<EmailLog> getEmailLogs(int page, int size);

    /**
     * 상태별 메일 이력 조회
     */
    List<EmailLog> getEmailLogsByStatus(String status, int page, int size);

    /**
     * 전체 메일 이력 건수
     */
    int getTotalEmailLogCount();

    /**
     * 실패한 메일 재발송
     */
    EmailLog resendEmail(Long emailLogId, String senderUsername);
}
