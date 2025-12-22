package com.taskflow.dto.transfer;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 업무 이관 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    /**
     * 이관받을 사용자 ID
     */
    @NotNull(message = "이관 대상자는 필수입니다.")
    private Long targetUserId;

    /**
     * 이관 사유 (선택)
     */
    private String reason;

    /**
     * 이관할 업무 ID 목록
     */
    @NotEmpty(message = "이관할 업무를 선택해주세요.")
    private List<Long> itemIds;
}
