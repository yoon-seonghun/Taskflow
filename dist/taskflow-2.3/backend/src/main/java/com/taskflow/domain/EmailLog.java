package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 메일 발송 이력 엔티티
 *
 * 테이블: TB_EMAIL_LOG
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    /**
     * 메일 로그 ID (PK)
     */
    private Long emailLogId;

    /**
     * 발송 유형
     */
    private String emailType;

    /**
     * 메일 제목
     */
    private String subject;

    /**
     * 수신자 이메일
     */
    private String recipientEmail;

    /**
     * 수신자 이름
     */
    private String recipientName;

    /**
     * 수신자 USERNAME (시스템 사용자)
     */
    private String recipientUsername;

    /**
     * 메일 본문
     */
    private String content;

    /**
     * 발송 상태
     */
    private String status;

    /**
     * 실패 시 에러 메시지
     */
    private String errorMessage;

    /**
     * 관련 대상 유형 (ITEM, BOARD)
     */
    private String relatedType;

    /**
     * 관련 대상 ID
     */
    private Long relatedId;

    /**
     * 발송일시
     */
    private LocalDateTime sentAt;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    // =============================================
    // 상수: 발송 유형
    // =============================================
    public static final String TYPE_TASK_ASSIGN = "TASK_ASSIGN";
    public static final String TYPE_TASK_TRANSFER = "TASK_TRANSFER";
    public static final String TYPE_TASK_SHARE = "TASK_SHARE";
    public static final String TYPE_DUE_DATE_ALERT = "DUE_DATE_ALERT";
    public static final String TYPE_MANUAL = "MANUAL";

    // =============================================
    // 상수: 발송 상태
    // =============================================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    // =============================================
    // 상수: 관련 대상 유형
    // =============================================
    public static final String RELATED_ITEM = "ITEM";
    public static final String RELATED_BOARD = "BOARD";

    /**
     * 발송 성공 처리
     */
    public void markAsSent() {
        this.status = STATUS_SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 발송 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = STATUS_FAILED;
        this.errorMessage = errorMessage;
    }
}
