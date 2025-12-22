package com.taskflow.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 이관 대상 업무 미리보기 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferPreviewResponse {

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 이관 대상 업무 목록
     */
    private List<PendingItem> pendingItems;

    /**
     * 이관 대상 업무 총 수
     */
    private Integer totalCount;

    /**
     * 이관 대상 업무 정보
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingItem {
        /**
         * 업무 ID
         */
        private Long itemId;

        /**
         * 업무 제목
         */
        private String title;

        /**
         * 상태
         */
        private String status;

        /**
         * 우선순위
         */
        private String priority;

        /**
         * 담당자명
         */
        private String assigneeName;

        /**
         * 상태 표시 텍스트
         */
        public String getStatusLabel() {
            switch (status) {
                case "NOT_STARTED": return "시작전";
                case "IN_PROGRESS": return "진행중";
                case "COMPLETED": return "완료";
                case "DELETED": return "삭제";
                default: return status;
            }
        }

        /**
         * 우선순위 표시 텍스트
         */
        public String getPriorityLabel() {
            switch (priority) {
                case "URGENT": return "긴급";
                case "HIGH": return "높음";
                case "NORMAL": return "보통";
                case "LOW": return "낮음";
                default: return priority;
            }
        }
    }
}
