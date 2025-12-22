package com.taskflow.dto.board;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보드 소유권 이전 요청 DTO
 * - 보드 소유권을 다른 사용자에게 이전
 * - 보드명이 "보드이관"으로 자동 변경됨
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardTransferRequest {

    /**
     * 이관받을 사용자 ID (필수)
     */
    @NotNull(message = "이관받을 사용자를 지정해주세요.")
    private Long targetUserId;

    /**
     * 이관 사유 (선택)
     */
    private String reason;
}
