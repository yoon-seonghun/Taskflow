package com.taskflow.mapper;

import com.taskflow.domain.ItemContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 업무 상세 내용 Mapper
 */
@Mapper
public interface ItemContentMapper {

    /**
     * 업무 ID로 내용 조회
     */
    ItemContent findByItemId(@Param("itemId") Long itemId);

    /**
     * 내용 존재 여부 확인
     */
    boolean existsByItemId(@Param("itemId") Long itemId);

    /**
     * 내용 등록
     */
    int insert(ItemContent content);

    /**
     * 내용 수정 (버전 충돌 체크)
     * @return 업데이트된 행 수 (0이면 버전 충돌)
     */
    int update(@Param("itemId") Long itemId,
               @Param("contentType") String contentType,
               @Param("content") String content,
               @Param("plainText") String plainText,
               @Param("version") Integer version,
               @Param("updatedBy") String updatedBy);

    /**
     * 내용 삭제
     */
    int deleteByItemId(@Param("itemId") Long itemId);

    /**
     * 현재 버전 조회
     */
    Integer getVersion(@Param("itemId") Long itemId);
}
