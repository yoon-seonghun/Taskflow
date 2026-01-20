package com.taskflow.dto.item;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 업무 완료 요청 DTO (v2.2)
 *
 * 하위 업무가 미완료인 경우 강제 완료 옵션 지원
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCompleteRequest {

    /**
     * 강제 완료 여부
     *
     * true: 미완료 하위 업무가 있어도 완료 처리
     * false/null: 미완료 하위 업무가 있으면 에러 반환
     */
    private Boolean forceComplete;

    /**
     * 강제 완료 여부 확인
     */
    public boolean isForceComplete() {
        return Boolean.TRUE.equals(forceComplete);
    }
}
