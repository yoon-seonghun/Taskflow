package com.taskflow.service;

import com.taskflow.dto.board.*;
import com.taskflow.dto.share.ShareRequest;
import com.taskflow.dto.share.ShareResponse;
import com.taskflow.dto.share.ShareUpdateRequest;
import com.taskflow.dto.transfer.TransferPreviewResponse;
import com.taskflow.dto.transfer.TransferRequest;
import com.taskflow.dto.transfer.TransferResultResponse;

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
     * 보드 상세 조회 (권한 정보 포함)
     *
     * @param boardId 보드 ID
     * @param userId  현재 사용자 ID
     * @return 보드 응답
     */
    BoardResponse getBoardWithPermission(Long boardId, Long userId);

    /**
     * 보드 목록 조회 (소유 보드 + 공유받은 보드 분리)
     *
     * @param userId 사용자 ID
     * @return 보드 목록 응답
     */
    BoardListResponse getBoardList(Long userId);

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

    // =============================================
    // 보드 관리 (신규 기능)
    // =============================================

    /**
     * 보드 순서 변경
     *
     * @param boardId   보드 ID
     * @param sortOrder 정렬 순서
     * @param userId    요청 사용자 ID
     */
    void updateBoardOrder(Long boardId, Integer sortOrder, Long userId);

    /**
     * 보드 삭제 (이관 포함)
     *
     * @param boardId 보드 ID
     * @param request 삭제 요청 (이관 정보 포함)
     * @param userId  요청 사용자 ID
     * @return 이관 결과 (이관된 경우)
     */
    TransferResultResponse deleteBoardWithTransfer(Long boardId, BoardDeleteRequest request, Long userId);

    /**
     * 이관 대상 업무 미리보기
     *
     * @param boardId 보드 ID
     * @return 이관 미리보기 응답
     */
    TransferPreviewResponse getTransferPreview(Long boardId);

    /**
     * 보드 공유 권한 변경
     *
     * @param boardId   보드 ID
     * @param userId    대상 사용자 ID
     * @param request   권한 변경 요청
     * @param requestUserId 요청 사용자 ID
     */
    void updateBoardSharePermission(Long boardId, Long userId, ShareUpdateRequest request, Long requestUserId);

    /**
     * 사용자의 보드 권한 조회
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 권한 (OWNER/VIEW/EDIT/FULL)
     */
    String getUserPermission(Long boardId, Long userId);

    /**
     * 수정 권한 확인
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 수정 가능 여부
     */
    boolean canEdit(Long boardId, Long userId);

    /**
     * 삭제 권한 확인
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 삭제 가능 여부
     */
    boolean canDelete(Long boardId, Long userId);

    /**
     * 보드 소유권 이전
     * - 보드를 다른 사용자에게 이관
     * - 보드명이 "보드이관"으로 자동 변경됨
     *
     * @param boardId   보드 ID
     * @param request   이전 요청
     * @param currentUserId 요청 사용자 ID
     * @return 이전된 보드 응답
     */
    BoardResponse transferBoardOwnership(Long boardId, BoardTransferRequest request, Long currentUserId);
}
