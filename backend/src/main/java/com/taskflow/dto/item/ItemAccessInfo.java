package com.taskflow.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 업무 접근 권한 정보 DTO
 *
 * 현재 사용자의 해당 업무에 대한 접근 권한 정보를 담습니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemAccessInfo {

    /**
     * 소유자 여부
     */
    private Boolean isOwner;

    /**
     * 공유 유형 (null: 소유자, SHARE: 공유, ASSIGN: 배당)
     */
    private String shareType;

    /**
     * 권한 수준 (null: 소유자, VIEW, EDIT, FULL)
     */
    private String permissionLevel;

    /**
     * 배당자 USERNAME (ASSIGN일 경우)
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

    // =============================================
    // 편집 가능 여부 (프론트엔드 UI 제어용)
    // =============================================

    /**
     * 제목 편집 가능 여부 (소유자만)
     */
    private Boolean canEditTitle;

    /**
     * 기본 속성 편집 가능 여부 (FULL 이상)
     */
    private Boolean canEditBasicProperties;

    /**
     * 사용자 속성 편집 가능 여부 (EDIT 이상)
     */
    private Boolean canEditUserProperties;

    /**
     * 담당자 배정 가능 여부 (소유자만)
     */
    private Boolean canAssign;

    /**
     * 삭제 가능 여부 (소유자만)
     */
    private Boolean canDelete;

    /**
     * 완료 처리 가능 여부 (EDIT 이상)
     */
    private Boolean canComplete;

    // =============================================
    // 정적 팩토리 메서드
    // =============================================

    /**
     * 소유자용 접근 정보 생성
     */
    public static ItemAccessInfo forOwner() {
        return ItemAccessInfo.builder()
                .isOwner(true)
                .shareType(null)
                .permissionLevel(null)
                .assignedBy(null)
                .assignedByName(null)
                .assignedAt(null)
                .canEditTitle(true)
                .canEditBasicProperties(true)
                .canEditUserProperties(true)
                .canAssign(true)
                .canDelete(true)
                .canComplete(true)
                .build();
    }

    /**
     * 공유/배정 사용자용 접근 정보 생성
     */
    public static ItemAccessInfo forSharedUser(String shareType, String permissionLevel,
                                                String assignedBy, String assignedByName, LocalDateTime assignedAt) {
        boolean isFullPermission = "FULL".equals(permissionLevel);
        boolean isEditOrHigher = "EDIT".equals(permissionLevel) || isFullPermission;

        return ItemAccessInfo.builder()
                .isOwner(false)
                .shareType(shareType)
                .permissionLevel(permissionLevel)
                .assignedBy(assignedBy)
                .assignedByName(assignedByName)
                .assignedAt(assignedAt)
                .canEditTitle(false)  // 소유자만 가능
                .canEditBasicProperties(isFullPermission)
                .canEditUserProperties(isEditOrHigher)
                .canAssign(false)  // 소유자만 가능
                .canDelete(false)  // 소유자만 가능
                .canComplete(isEditOrHigher)
                .build();
    }

    /**
     * 접근 불가 정보 생성
     */
    public static ItemAccessInfo noAccess() {
        return ItemAccessInfo.builder()
                .isOwner(false)
                .canEditTitle(false)
                .canEditBasicProperties(false)
                .canEditUserProperties(false)
                .canAssign(false)
                .canDelete(false)
                .canComplete(false)
                .build();
    }
}
