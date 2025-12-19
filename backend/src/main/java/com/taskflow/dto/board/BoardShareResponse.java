package com.taskflow.dto.board;

import com.taskflow.domain.BoardShare;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 보드 공유 응답 DTO
 */
@Getter
@Builder
public class BoardShareResponse {

    /**
     * 보드 공유 ID
     */
    private Long boardShareId;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 사용자명
     */
    private String userName;

    /**
     * 로그인 ID
     */
    private String loginId;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 권한 레벨
     */
    private String permission;

    /**
     * 공유일시
     */
    private LocalDateTime createdAt;

    /**
     * BoardShare 도메인에서 변환
     */
    public static BoardShareResponse from(BoardShare boardShare) {
        if (boardShare == null) {
            return null;
        }

        return BoardShareResponse.builder()
                .boardShareId(boardShare.getBoardShareId())
                .boardId(boardShare.getBoardId())
                .boardName(boardShare.getBoardName())
                .userId(boardShare.getUserId())
                .userName(boardShare.getUserName())
                .loginId(boardShare.getLoginId())
                .departmentName(boardShare.getDepartmentName())
                .permission(boardShare.getPermission())
                .createdAt(boardShare.getCreatedAt())
                .build();
    }

    /**
     * BoardShare 리스트에서 변환
     */
    public static List<BoardShareResponse> fromList(List<BoardShare> shares) {
        if (shares == null) {
            return List.of();
        }

        return shares.stream()
                .map(BoardShareResponse::from)
                .collect(Collectors.toList());
    }
}
