package com.taskflow.dto.item;

import com.taskflow.domain.ItemShare;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 업무 공유 응답 DTO
 */
@Getter
@Builder
public class ItemShareResponse {

    /**
     * 공유 ID
     */
    private Long itemShareId;

    /**
     * 업무 ID
     */
    private Long itemId;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 로그인 ID
     */
    private String loginId;

    /**
     * 사용자 이름
     */
    private String userName;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 권한 레벨
     */
    private String permission;

    /**
     * 조회 가능 여부
     */
    private boolean canView;

    /**
     * 편집 가능 여부
     */
    private boolean canEdit;

    /**
     * 삭제 가능 여부
     */
    private boolean canDelete;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 도메인 객체를 응답 DTO로 변환
     */
    public static ItemShareResponse from(ItemShare share) {
        if (share == null) {
            return null;
        }

        return ItemShareResponse.builder()
                .itemShareId(share.getItemShareId())
                .itemId(share.getItemId())
                .userId(share.getUserId())
                .loginId(share.getLoginId())
                .userName(share.getUserName())
                .departmentName(share.getDepartmentName())
                .permission(share.getPermission())
                .canView(share.canView())
                .canEdit(share.canEdit())
                .canDelete(share.canDelete())
                .createdAt(share.getCreatedAt())
                .updatedAt(share.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 객체 리스트를 응답 DTO 리스트로 변환
     */
    public static List<ItemShareResponse> fromList(List<ItemShare> shares) {
        if (shares == null) {
            return List.of();
        }

        return shares.stream()
                .map(ItemShareResponse::from)
                .collect(Collectors.toList());
    }
}
