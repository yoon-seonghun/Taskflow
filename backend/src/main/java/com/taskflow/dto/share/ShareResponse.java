package com.taskflow.dto.share;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 공유 정보 응답 DTO
 * 보드 공유, 업무 공유에 공통 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 로그인 ID
     */
    private String loginId;

    /**
     * 사용자명
     */
    private String userName;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 권한 (VIEW/EDIT/FULL)
     */
    private String permission;

    /**
     * 공유일시
     */
    private LocalDateTime createdAt;

    /**
     * 권한 레벨 (비교용)
     */
    public int getPermissionLevel() {
        if ("FULL".equals(permission)) return 3;
        if ("EDIT".equals(permission)) return 2;
        if ("VIEW".equals(permission)) return 1;
        return 0;
    }

    /**
     * 권한 표시 텍스트
     */
    public String getPermissionLabel() {
        switch (permission) {
            case "FULL": return "전체 권한";
            case "EDIT": return "편집 권한";
            case "VIEW": return "조회 권한";
            default: return permission;
        }
    }
}
