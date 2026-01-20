package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 이벤트 공유 도메인
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShare {

    public static final String TYPE_VIEW = "VIEW";
    public static final String TYPE_EDIT = "EDIT";
    public static final String TYPE_TRANSFER = "TRANSFER";

    private Long eventShareId;
    private Long eventId;
    private String shareType;
    private String username;
    private String sharedBy;
    private LocalDateTime sharedAt;
    private LocalDateTime deletedAt;

    // 감사 필드
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // 조인 필드 (조회용)
    private String userName;            // 공유 대상자 이름
    private String departmentName;      // 공유 대상자 부서
    private String sharedByName;        // 공유해준 사용자 이름
    private String eventTitle;          // 이벤트 제목

    // Lombok 호환성: username/userName 패턴 충돌 방지를 위한 명시적 getter
    public String getUsername() {
        return username;
    }

    public String getUserName() {
        return userName;
    }

    public String getSharedBy() {
        return sharedBy;
    }

    public String getSharedByName() {
        return sharedByName;
    }
}
