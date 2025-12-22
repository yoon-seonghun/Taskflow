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
    List<ItemShare> selectByUserId(@Param("userId") Long userId);

    /**
     * 특정 업무-사용자 공유 조회
     */
    ItemShare selectByItemIdAndUserId(
            @Param("itemId") Long itemId,
            @Param("userId") Long userId
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
            @Param("userId") Long userId,
            @Param("permission") String permission,
            @Param("updatedBy") Long updatedBy
    );

    /**
     * 공유 제거
     */
    int delete(
            @Param("itemId") Long itemId,
            @Param("userId") Long userId
    );

    /**
     * 업무의 모든 공유 제거
     */
    int deleteByItemId(@Param("itemId") Long itemId);

    /**
     * 중복 체크
     */
    boolean existsByItemIdAndUserId(
            @Param("itemId") Long itemId,
            @Param("userId") Long userId
    );

    /**
     * 사용자의 업무 접근 권한 확인
     */
    String getPermission(
            @Param("itemId") Long itemId,
            @Param("userId") Long userId
    );
}
