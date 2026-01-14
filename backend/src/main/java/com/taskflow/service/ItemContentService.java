package com.taskflow.service;

import com.taskflow.dto.content.ItemContentResponse;
import com.taskflow.dto.content.ItemContentUpdateRequest;

/**
 * 업무 상세 내용 서비스 인터페이스
 */
public interface ItemContentService {

    /**
     * 업무 내용 조회
     *
     * @param boardId 보드 ID (권한 체크용)
     * @param itemId  업무 ID
     * @return 업무 내용 (없으면 null)
     */
    ItemContentResponse getContent(Long boardId, Long itemId);

    /**
     * 업무 내용 저장/수정
     *
     * @param boardId 보드 ID (권한 체크용)
     * @param itemId  업무 ID
     * @param request 저장 요청
     * @return 저장된 업무 내용
     */
    ItemContentResponse updateContent(Long boardId, Long itemId, ItemContentUpdateRequest request);

    /**
     * 업무 내용 삭제
     *
     * @param itemId 업무 ID
     */
    void deleteContent(Long itemId);

    /**
     * 업무에 내용이 있는지 확인
     *
     * @param itemId 업무 ID
     * @return 내용 존재 여부
     */
    boolean hasContent(Long itemId);
}
