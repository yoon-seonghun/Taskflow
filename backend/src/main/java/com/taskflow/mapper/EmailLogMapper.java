package com.taskflow.mapper;

import com.taskflow.domain.EmailLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메일 발송 이력 Mapper
 */
@Mapper
public interface EmailLogMapper {

    /**
     * 메일 로그 저장
     */
    int insert(EmailLog emailLog);

    /**
     * 메일 로그 상태 업데이트
     */
    int updateStatus(
            @Param("emailLogId") Long emailLogId,
            @Param("status") String status,
            @Param("errorMessage") String errorMessage,
            @Param("sentAt") LocalDateTime sentAt
    );

    /**
     * ID로 조회
     */
    EmailLog selectById(@Param("emailLogId") Long emailLogId);

    /**
     * 발송 유형별 목록 조회
     */
    List<EmailLog> selectByType(
            @Param("emailType") String emailType,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 상태별 목록 조회
     */
    List<EmailLog> selectByStatus(
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 관련 대상별 목록 조회
     */
    List<EmailLog> selectByRelated(
            @Param("relatedType") String relatedType,
            @Param("relatedId") Long relatedId
    );

    /**
     * 기간별 목록 조회
     */
    List<EmailLog> selectByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 전체 목록 조회 (페이징)
     */
    List<EmailLog> selectAll(
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 전체 건수 조회
     */
    int countAll();

    /**
     * 상태별 건수 조회
     */
    int countByStatus(@Param("status") String status);

    /**
     * PENDING 상태 메일 목록 조회 (재발송용)
     */
    List<EmailLog> selectPendingEmails(@Param("limit") int limit);
}
