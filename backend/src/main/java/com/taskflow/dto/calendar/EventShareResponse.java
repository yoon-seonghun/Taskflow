package com.taskflow.dto.calendar;

import com.taskflow.domain.EventShare;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 이벤트 공유 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShareResponse {

    private Long eventShareId;
    private Long eventId;
    private String eventTitle;
    private String shareType;
    private String username;
    private String userName;
    private String departmentName;
    private String sharedBy;
    private String sharedByName;
    private LocalDateTime sharedAt;

    public static EventShareResponse from(EventShare share) {
        return EventShareResponse.builder()
                .eventShareId(share.getEventShareId())
                .eventId(share.getEventId())
                .eventTitle(share.getEventTitle())
                .shareType(share.getShareType())
                .username(share.getUsername())
                .userName(share.getUserName())
                .departmentName(share.getDepartmentName())
                .sharedBy(share.getSharedBy())
                .sharedByName(share.getSharedByName())
                .sharedAt(share.getSharedAt())
                .build();
    }

    public static List<EventShareResponse> fromList(List<EventShare> shares) {
        return shares.stream()
                .map(EventShareResponse::from)
                .collect(Collectors.toList());
    }
}
