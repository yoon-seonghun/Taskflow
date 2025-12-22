package com.taskflow.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 감사 로그 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    /**
     * 로그 ID
     */
    private Long logId;

    /**
     * 대상 유형 (BOARD/ITEM/BOARD_SHARE/ITEM_SHARE)
     */
    private String targetType;

    /**
     * 대상 ID
     */
    private Long targetId;

    /**
     * 대상 이름
     */
    private String targetName;

    /**
     * 작업 유형 (CREATE/UPDATE/DELETE/TRANSFER/SHARE/UNSHARE)
     */
    private String action;

    /**
     * 수행자 ID
     */
    private Long actorId;

    /**
     * 수행자명
     */
    private String actorName;

    /**
     * 변경 내용 설명
     */
    private String description;

    /**
     * 관련 사용자 ID
     */
    private Long relatedUserId;

    /**
     * 관련 사용자명
     */
    private String relatedUserName;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 대상 유형 표시 텍스트
     */
    public String getTargetTypeLabel() {
        switch (targetType) {
            case "BOARD": return "보드";
            case "ITEM": return "업무";
            case "BOARD_SHARE": return "보드 공유";
            case "ITEM_SHARE": return "업무 공유";
            default: return targetType;
        }
    }

    /**
     * 작업 유형 표시 텍스트
     */
    public String getActionLabel() {
        switch (action) {
            case "CREATE": return "생성";
            case "UPDATE": return "수정";
            case "DELETE": return "삭제";
            case "TRANSFER": return "이관";
            case "SHARE": return "공유";
            case "UNSHARE": return "공유 해제";
            default: return action;
        }
    }
}
