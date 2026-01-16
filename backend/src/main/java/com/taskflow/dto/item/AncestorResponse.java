package com.taskflow.dto.item;

import com.taskflow.domain.Item;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상위 업무 경로 응답 DTO (v2.2)
 *
 * Breadcrumb 표시용 상위 계층 정보
 */
@Getter
@Builder
public class AncestorResponse {

    /**
     * 업무 ID
     */
    private Long itemId;

    /**
     * 업무 깊이
     */
    private Integer itemDepth;

    /**
     * 업무 제목
     */
    private String title;

    /**
     * 업무 상태
     */
    private String status;

    /**
     * 도메인 객체를 응답 DTO로 변환
     */
    public static AncestorResponse from(Item item) {
        if (item == null) {
            return null;
        }

        return AncestorResponse.builder()
                .itemId(item.getItemId())
                .itemDepth(item.getItemDepth() != null ? item.getItemDepth() : 0)
                .title(item.getTitle() != null ? item.getTitle() : item.getContent())
                .status(item.getStatus())
                .build();
    }

    /**
     * 도메인 객체 리스트를 응답 DTO 리스트로 변환
     */
    public static List<AncestorResponse> fromList(List<Item> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(AncestorResponse::from)
                .collect(Collectors.toList());
    }
}
