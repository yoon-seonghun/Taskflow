package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 업무 상세 내용 엔티티
 *
 * 테이블: TB_ITEM_CONTENT
 *
 * 업무(Item)의 리치 텍스트 내용을 별도로 관리
 * - 목록 조회 성능 최적화
 * - 대용량 컨텐츠 지원 (MEDIUMTEXT)
 * - 동시 편집 충돌 방지 (VERSION)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemContent {

    /**
     * 컨텐츠 ID (PK)
     */
    private Long contentId;

    /**
     * 업무 ID (FK → TB_ITEM)
     */
    private Long itemId;

    /**
     * 컨텐츠 타입
     * - HTML: TipTap HTML 출력
     * - JSON: TipTap JSON 출력
     * - MARKDOWN: 마크다운 형식
     */
    private String contentType;

    /**
     * 컨텐츠 본문 (리치 텍스트)
     */
    private String content;

    /**
     * 검색용 평문 텍스트
     */
    private String plainText;

    /**
     * 버전 (동시 편집 충돌 방지)
     */
    private Integer version;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;
}
