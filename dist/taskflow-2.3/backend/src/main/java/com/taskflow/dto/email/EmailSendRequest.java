package com.taskflow.dto.email;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 수동 메일 발송 요청 DTO
 */
@Getter
@Setter
public class EmailSendRequest {

    /**
     * 수신자 목록
     */
    @NotEmpty(message = "수신자가 필요합니다")
    @Valid
    private List<Recipient> recipients;

    /**
     * 메일 제목
     */
    @NotBlank(message = "메일 제목은 필수입니다")
    private String subject;

    /**
     * 메일 본문
     */
    @NotBlank(message = "메일 내용은 필수입니다")
    private String content;

    /**
     * 관련 대상 유형 (ITEM, BOARD) - 선택
     */
    private String relatedType;

    /**
     * 관련 대상 ID - 선택
     */
    private Long relatedId;

    /**
     * 수신자 정보
     */
    @Getter
    @Setter
    public static class Recipient {
        @NotBlank(message = "이메일 주소는 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        private String name;
    }
}
