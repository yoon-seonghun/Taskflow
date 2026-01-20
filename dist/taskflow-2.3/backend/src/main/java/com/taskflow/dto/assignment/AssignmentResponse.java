package com.taskflow.dto.assignment;

import com.taskflow.domain.ItemShare;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 업무 배정 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {

    /**
     * 공유 ID
     */
    private Long shareId;

    /**
     * 업무 ID
     */
    private Long itemId;

    /**
     * 담당자 USERNAME
     */
    private String assigneeUsername;

    /**
     * 담당자 이름
     */
    private String assigneeName;

    /**
     * 공유 유형 (SHARE, ASSIGN)
     */
    private String shareType;

    /**
     * 권한 수준 (VIEW, EDIT, FULL)
     */
    private String permissionLevel;

    /**
     * 배당자 USERNAME
     */
    private String assignedBy;

    /**
     * 배당자 이름
     */
    private String assignedByName;

    /**
     * 배당 일시
     */
    private LocalDateTime assignedAt;

    /**
     * 이메일 발송 여부
     */
    private Boolean emailSent;

    /**
     * 알림 발송 여부
     */
    private Boolean notificationSent;

    /**
     * ItemShare 엔티티로부터 생성
     */
    public static AssignmentResponse from(ItemShare itemShare) {
        return AssignmentResponse.builder()
                .shareId(itemShare.getItemShareId())
                .itemId(itemShare.getItemId())
                .assigneeUsername(itemShare.getUsername())
                .assigneeName(itemShare.getUserName())
                .shareType(itemShare.getShareType())
                .permissionLevel(itemShare.getPermission())
                .assignedBy(itemShare.getAssignedBy())
                .assignedByName(itemShare.getAssignedByName())
                .assignedAt(itemShare.getAssignedAt())
                .build();
    }
}
