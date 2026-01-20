package com.taskflow.mapper;

import com.taskflow.domain.EventShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 이벤트 공유 Mapper
 */
@Mapper
public interface EventShareMapper {

    /**
     * 공유 추가
     */
    void insert(EventShare share);

    /**
     * 공유 해제 (논리 삭제)
     */
    int delete(@Param("eventId") Long eventId, @Param("username") String username, @Param("deletedBy") String deletedBy);

    /**
     * 공유 타입 변경
     */
    int updateShareType(@Param("eventId") Long eventId, @Param("username") String username,
                        @Param("shareType") String shareType, @Param("updatedBy") String updatedBy);

    /**
     * 이벤트 공유 목록 조회
     */
    List<EventShare> selectByEventId(@Param("eventId") Long eventId);

    /**
     * 특정 사용자의 공유 정보 조회
     */
    Optional<EventShare> findByEventIdAndUsername(@Param("eventId") Long eventId, @Param("username") String username);

    /**
     * 공유 존재 여부 확인
     */
    boolean exists(@Param("eventId") Long eventId, @Param("username") String username);

    /**
     * 공유 권한 조회
     */
    String getPermission(@Param("eventId") Long eventId, @Param("username") String username);

    /**
     * 이벤트의 모든 공유 해제
     */
    int deleteAllByEventId(@Param("eventId") Long eventId, @Param("deletedBy") String deletedBy);

    /**
     * 이관 이력 추가 (TRANSFER 타입)
     */
    void insertTransferHistory(EventShare share);

    /**
     * 사용자가 공유받은 이벤트 목록
     */
    List<EventShare> selectSharedToMe(@Param("username") String username);
}
