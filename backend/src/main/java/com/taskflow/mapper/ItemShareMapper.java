package com.taskflow.mapper;

import com.taskflow.domain.ItemShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 업무 공유 Mapper
 */
@Mapper
public interface ItemShareMapper {

    /**
     * 업무별 공유 목록 조회
     */
    List<ItemShare> selectByItemId(@Param("itemId") Long itemId);

    /**
     * 사용자가 공유받은 업무 목록 조회
     */
    List<ItemShare> selectByUsername(@Param("username") String username);

    /**
     * 사용자가 공유받은 업무 목록 조회 (공유 유형 필터링)
     */
    List<ItemShare> selectByUsernameAndShareType(
            @Param("username") String username,
            @Param("shareType") String shareType
    );

    /**
     * 특정 업무-사용자 공유 조회
     */
    ItemShare selectByItemIdAndUsername(
            @Param("itemId") Long itemId,
            @Param("username") String username
    );

    /**
     * 공유 추가
     */
    int insert(ItemShare itemShare);

    /**
     * 권한 변경
     */
    int updatePermission(
            @Param("itemId") Long itemId,
            @Param("username") String username,
            @Param("permission") String permission,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 배정 정보 전체 업데이트
     */
    int updateAssignment(
            @Param("itemId") Long itemId,
            @Param("username") String username,
            @Param("shareType") String shareType,
            @Param("permission") String permission,
            @Param("assignedBy") String assignedBy,
            @Param("assignedAt") java.time.LocalDateTime assignedAt,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 공유 제거
     */
    int delete(
            @Param("itemId") Long itemId,
            @Param("username") String username
    );

    /**
     * 업무의 모든 공유 제거
     */
    int deleteByItemId(@Param("itemId") Long itemId);

    /**
     * 중복 체크
     */
    boolean existsByItemIdAndUsername(
            @Param("itemId") Long itemId,
            @Param("username") String username
    );

    /**
     * 사용자의 업무 접근 권한 확인
     */
    String getPermission(
            @Param("itemId") Long itemId,
            @Param("username") String username
    );
}
