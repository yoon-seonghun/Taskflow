package com.taskflow.dto.board;

import com.taskflow.domain.Board;
import com.taskflow.dto.group.GroupResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 보드 응답 DTO
 */
@Getter
@Builder
public class BoardResponse {

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 보드 설명
     */
    private String description;

    /**
     * 소유자 ID
     */
    private Long ownerId;

    /**
     * 소유자명
     */
    private String ownerName;

    /**
     * 기본 뷰 타입
     */
    private String defaultView;

    /**
     * 표시 색상
     */
    private String color;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * 아이템 수
     */
    private Integer itemCount;

    /**
     * 공유 사용자 수
     */
    private Integer shareCount;

    /**
     * 미완료 업무 수 (이관 대상)
     */
    private Integer pendingItemCount;

    /**
     * 현재 사용자의 권한 (OWNER/VIEW/EDIT/FULL)
     */
    private String currentUserPermission;

    /**
     * 현재 사용자가 소유자인지 여부
     */
    private Boolean isOwner;

    /**
     * 사용 가능한 그룹 목록
     */
    private List<GroupResponse> groups;

    /**
     * 담당자로 지정 가능한 사용자 목록 (소유자 + 공유 사용자)
     */
    private List<SharedUserInfo> sharedUsers;

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
    public static BoardResponse from(Board board) {
        if (board == null) {
            return null;
        }

        return BoardResponse.builder()
                .boardId(board.getBoardId())
                .boardName(board.getBoardName())
                .description(board.getDescription())
                .ownerId(board.getOwnerId())
                .ownerName(board.getOwnerName())
                .defaultView(board.getDefaultView())
                .color(board.getColor())
                .sortOrder(board.getSortOrder())
                .useYn(board.getUseYn())
                .itemCount(board.getItemCount())
                .shareCount(board.getShareCount())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 객체 리스트를 응답 DTO 리스트로 변환
     */
    public static List<BoardResponse> fromList(List<Board> boards) {
        if (boards == null) {
            return List.of();
        }

        return boards.stream()
                .map(BoardResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 담당자 지정용 사용자 정보 DTO
     */
    @Getter
    @Builder
    public static class SharedUserInfo {
        private Long userId;
        private String userName;
        private String departmentName;
    }
}
