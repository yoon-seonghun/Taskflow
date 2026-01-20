package com.taskflow.dto.property;

import com.taskflow.domain.PropertyOption;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 옵션 상세 응답 DTO
 */
@Getter
@Builder
public class OptionDetailResponse {

    /**
     * 옵션 ID
     */
    private Long optionId;

    /**
     * 속성 정의 ID
     */
    private Long propertyId;

    /**
     * 속성명
     */
    private String propertyName;

    /**
     * 속성 타입
     */
    private String propertyType;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 옵션명 (OPTION_LABEL)
     */
    private String optionName;

    /**
     * 표시 색상
     */
    private String color;

    /**
     * 표시 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * 사용 중인 아이템 수
     */
    private Integer usageCount;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 도메인 객체에서 변환
     */
    public static OptionDetailResponse from(PropertyOption option) {
        if (option == null) {
            return null;
        }

        return OptionDetailResponse.builder()
                .optionId(option.getOptionId())
                .propertyId(option.getPropertyId())
                .propertyName(option.getPropertyName())
                .propertyType(option.getPropertyType())
                .boardId(option.getBoardId())
                .optionName(option.getOptionName())
                .color(option.getColor())
                .sortOrder(option.getSortOrder())
                .useYn(option.getUseYn())
                .usageCount(option.getUsageCount())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 객체 리스트에서 변환
     */
    public static List<OptionDetailResponse> fromList(List<PropertyOption> options) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
                .map(OptionDetailResponse::from)
                .collect(Collectors.toList());
    }
}
