package com.taskflow.dto.board;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskflow.domain.BoardShare;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 보드 공유 응답 DTO
 */
@Slf4j
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
     * 사용자 USERNAME
     */
    private String username;

    /**
     * 사용자명
     */
    @JsonProperty("user_name")
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

        String userName = boardShare.getUserName();
        log.debug("BoardShareResponse.from() - username: {}, userName: {}",
                  boardShare.getUsername(), userName);

        return BoardShareResponse.builder()
                .boardShareId(boardShare.getBoardShareId())
                .boardId(boardShare.getBoardId())
                .boardName(boardShare.getBoardName())
                .username(boardShare.getUsername())
                .userName(userName)
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
