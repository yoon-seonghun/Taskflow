package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 캘린더 (이벤트 그룹) 도메인
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Calendar {

    private Long calendarId;
    private String ownerUsername;
    private String calendarName;
    private String description;
    private String color;
    private Boolean isDefault;
    private Integer sortOrder;
    private String useYn;

    // 감사 필드
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // 조인 필드 (조회용)
    private String ownerName;           // 소유자 이름
    private Integer eventCount;         // 이벤트 수
    private Boolean isShared;           // 공유받은 캘린더 여부
    private String sharedByUsername;    // 공유해준 사용자
    private String sharedByName;        // 공유해준 사용자 이름
    private String shareType;           // 공유 타입 (VIEW/EDIT)
}
