package com.taskflow.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 댓글 생성 요청 DTO
 */
@Getter
@Setter
public class CommentCreateRequest {

    /**
     * 댓글 내용
     */
    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 2000, message = "댓글은 2000자를 초과할 수 없습니다")
    private String content;
}
