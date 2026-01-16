package com.taskflow.dto.item;

import com.taskflow.domain.Item;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 미완료 하위 업무 정보 응답 DTO (v2.2)
 *
 * 부모 업무 완료 시 미완료 하위 업무가 있을 때 반환
 */
@Getter
@Builder
public class IncompleteChildrenResponse {

    /**
     * 미완료 하위 업무 수
     */
    private Integer incompleteChildCount;

    /**
     * 미완료 하위 업무 목록 (간략 정보)
     */
    private List<ChildSummary> children;

    /**
     * 하위 업무 간략 정보
     */
    @Getter
    @Builder
    public static class ChildSummary {
        private Long itemId;
        private String title;
        private String status;
        private Integer itemDepth;
    }

    /**
     * 미완료 하위 업무 목록으로 응답 생성
     */
    public static IncompleteChildrenResponse from(List<Item> incompleteChildren) {
        if (incompleteChildren == null || incompleteChildren.isEmpty()) {
            return null;
        }

        List<ChildSummary> summaries = incompleteChildren.stream()
                .map(item -> ChildSummary.builder()
                        .itemId(item.getItemId())
                        .title(item.getTitle() != null ? item.getTitle() : item.getContent())
                        .status(item.getStatus())
                        .itemDepth(item.getItemDepth())
                        .build())
                .collect(Collectors.toList());

        return IncompleteChildrenResponse.builder()
                .incompleteChildCount(incompleteChildren.size())
                .children(summaries)
                .build();
    }
}
