package com.taskflow.service;

import com.taskflow.dto.board.*;

import java.util.List;

/**
 * 보드 서비스 인터페이스
 */
public interface BoardService {

    // =============================================
    // 보드 조회
    // =============================================

    /**
     * 보드 ID로 조회
     *
     * @param boardId 보드 ID
     * @return 보드 응답
     */
    BoardResponse getBoard(Long boardId);

    /**
     * 사용자가 접근 가능한 보드 목록 조회
     * (소유한 보드 + 공유받은 보드)
     *
     * @param userId 사용자 ID
     * @param useYn  사용 여부 필터
     * @return 보드 목록
     */
    List<BoardResponse> getAccessibleBoards(Long userId, String useYn);

    /**
     * 사용자가 소유한 보드 목록 조회
     *
     * @param ownerId 소유자 ID
     * @param useYn   사용 여부 필터
     * @return 보드 목록
     */
    List<BoardResponse> getOwnedBoards(Long ownerId, String useYn);

    /**
     * 전체 보드 목록 조회 (관리자용)
     *
     * @param useYn 사용 여부 필터
     * @return 보드 목록
     */
    List<BoardResponse> getAllBoards(String useYn);

    // =============================================
    // 보드 등록/수정/삭제
    // =============================================

    /**
     * 보드 등록
     *
     * @param request   등록 요청
     * @param createdBy 생성자 ID
     * @return 생성된 보드 응답
     */
    BoardResponse createBoard(BoardCreateRequest request, Long createdBy);

    /**
     * 보드 수정
     *
     * @param boardId   보드 ID
     * @param request   수정 요청
     * @param updatedBy 수정자 ID
     * @return 수정된 보드 응답
     */
    BoardResponse updateBoard(Long boardId, BoardUpdateRequest request, Long updatedBy);

    /**
     * 보드 삭제
     *
     * @param boardId 보드 ID
     * @param userId  요청 사용자 ID (소유자 확인용)
     */
    void deleteBoard(Long boardId, Long userId);

    // =============================================
    // 보드 공유 관리
    // =============================================

    /**
     * 보드 공유 사용자 목록 조회
     *
     * @param boardId 보드 ID
     * @return 공유 사용자 목록
     */
    List<BoardShareResponse> getBoardShares(Long boardId);

    /**
     * 보드 공유 추가
     *
     * @param boardId   보드 ID
     * @param request   공유 요청
     * @param createdBy 생성자 ID
     * @return 공유 정보
     */
    BoardShareResponse addBoardShare(Long boardId, BoardShareRequest request, Long createdBy);

    /**
     * 보드 공유 제거
     *
     * @param boardId 보드 ID
     * @param userId  제거할 사용자 ID
     * @param requestUserId 요청 사용자 ID (소유자 확인용)
     */
    void removeBoardShare(Long boardId, Long userId, Long requestUserId);

    // =============================================
    // 검증
    // =============================================

    /**
     * 사용자가 보드에 접근 가능한지 확인
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 접근 가능 여부
     */
    boolean hasAccess(Long boardId, Long userId);

    /**
     * 사용자가 보드 소유자인지 확인
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 소유자 여부
     */
    boolean isOwner(Long boardId, Long userId);
}
