package com.taskflow.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 업무 내용 저장/수정 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemContentUpdateRequest {

    /**
     * 컨텐츠 타입 (HTML, JSON, MARKDOWN)
     */
    @NotBlank(message = "컨텐츠 타입은 필수입니다.")
    private String contentType;

    /**
     * 컨텐츠 본문
     */
    @NotNull(message = "컨텐츠는 필수입니다.")
    private String content;

    /**
     * 현재 버전 (동시 편집 충돌 감지용)
     */
    @NotNull(message = "버전 정보는 필수입니다.")
    private Integer version;
}
