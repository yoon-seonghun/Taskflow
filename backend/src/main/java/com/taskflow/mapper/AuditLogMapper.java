package com.taskflow.mapper;

import com.taskflow.domain.AuditLog;
import com.taskflow.dto.audit.AuditLogSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 감사 로그 Mapper
 */
@Mapper
public interface AuditLogMapper {

    /**
     * 로그 기록
     */
    int insert(AuditLog auditLog);

    /**
     * 검색 조건에 따른 로그 조회
     */
    List<AuditLog> selectBySearchRequest(AuditLogSearchRequest request);

    /**
     * 검색 조건에 따른 총 건수
     */
    long countBySearchRequest(AuditLogSearchRequest request);

    /**
     * 대상별 로그 조회
     */
    List<AuditLog> selectByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    /**
     * 보드별 로그 조회 (보드 + 관련 업무 + 공유)
     */
    List<AuditLog> selectByBoardId(@Param("boardId") Long boardId);

    /**
     * 업무별 로그 조회 (업무 + 공유)
     */
    List<AuditLog> selectByItemId(@Param("itemId") Long itemId);

    /**
     * 수행자별 로그 조회
     */
    List<AuditLog> selectByActorId(
            @Param("actorId") Long actorId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 최근 로그 조회
     */
    List<AuditLog> selectRecent(@Param("limit") int limit);
}
