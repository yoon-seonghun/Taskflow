package com.taskflow.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 보드 목록 응답 DTO
 * 내 보드와 공유받은 보드를 구분하여 반환
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardListResponse {

    /**
     * 내가 소유한 보드 목록
     */
    private List<BoardResponse> ownedBoards;

    /**
     * 공유받은 보드 목록
     */
    private List<BoardResponse> sharedBoards;

    /**
     * 소유 보드 총 개수
     */
    private int totalOwnedCount;

    /**
     * 공유받은 보드 총 개수
     */
    private int totalSharedCount;
}
