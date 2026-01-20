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
     * @param username 현재 사용자 Username
     * @return 보드 응답
     */
    BoardResponse getBoardWithPermission(Long boardId, String username);

    /**
     * 보드 목록 조회 (소유 보드 + 공유받은 보드 분리)
     *
     * @param username 사용자 Username
     * @return 보드 목록 응답
     */
    BoardListResponse getBoardList(String username);

    /**
     * 사용자가 접근 가능한 보드 목록 조회
     * (소유한 보드 + 공유받은 보드)
     *
     * @param username 사용자 Username
     * @param useYn  사용 여부 필터
     * @return 보드 목록
     */
    List<BoardResponse> getAccessibleBoards(String username, String useYn);

    /**
     * 사용자가 소유한 보드 목록 조회
     *
     * @param ownerUsername 소유자 Username
     * @param useYn   사용 여부 필터
     * @return 보드 목록
     */
    List<BoardResponse> getOwnedBoards(String ownerUsername, String useYn);

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
     * @param createdBy 생성자 Username
     * @return 생성된 보드 응답
     */
    BoardResponse createBoard(BoardCreateRequest request, String createdBy);

    /**
     * 보드 수정
     *
     * @param boardId   보드 ID
     * @param request   수정 요청
     * @param updatedBy 수정자 Username
     * @return 수정된 보드 응답
     */
    BoardResponse updateBoard(Long boardId, BoardUpdateRequest request, String updatedBy);

    /**
     * 보드 삭제
     *
     * @param boardId 보드 ID
     * @param username 요청 사용자 Username (소유자 확인용)
     */
    void deleteBoard(Long boardId, String username);

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
     * @param createdBy 생성자 Username
     * @return 공유 정보
     */
    BoardShareResponse addBoardShare(Long boardId, BoardShareRequest request, String createdBy);

    /**
     * 보드 공유 제거
     *
     * @param boardId 보드 ID
     * @param username 제거할 사용자 Username
     * @param requestUsername 요청 사용자 Username (소유자 확인용)
     */
    void removeBoardShare(Long boardId, String username, String requestUsername);

    // =============================================
    // 검증
    // =============================================

    /**
     * 사용자가 보드에 접근 가능한지 확인
     *
     * @param boardId 보드 ID
     * @param username 사용자 Username
     * @return 접근 가능 여부
     */
    boolean hasAccess(Long boardId, String username);

    /**
     * 사용자가 보드 소유자인지 확인
     *
     * @param boardId 보드 ID
     * @param username 사용자 Username
     * @return 소유자 여부
     */
    boolean isOwner(Long boardId, String username);

    // =============================================
    // 보드 관리 (신규 기능)
    // =============================================

    /**
     * 보드 순서 변경 (소유 보드)
     *
     * @param boardId   보드 ID
     * @param sortOrder 정렬 순서
     * @param username  요청 사용자 Username
     */
    void updateBoardOrder(Long boardId, Integer sortOrder, String username);

    /**
     * 공유받은 보드 순서 변경
     *
     * @param boardId   보드 ID
     * @param sortOrder 정렬 순서
     * @param username  요청 사용자 Username
     */
    void updateSharedBoardOrder(Long boardId, Integer sortOrder, String username);

    /**
     * 보드 삭제 (이관 포함)
     *
     * @param boardId 보드 ID
     * @param request 삭제 요청 (이관 정보 포함)
     * @param username 요청 사용자 Username
     * @return 이관 결과 (이관된 경우)
     */
    TransferResultResponse deleteBoardWithTransfer(Long boardId, BoardDeleteRequest request, String username);

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
     * @param username  대상 사용자 Username
     * @param request   권한 변경 요청
     * @param requestUsername 요청 사용자 Username
     */
    void updateBoardSharePermission(Long boardId, String username, ShareUpdateRequest request, String requestUsername);

    /**
     * 사용자의 보드 권한 조회
     *
     * @param boardId 보드 ID
     * @param username 사용자 Username
     * @return 권한 (OWNER/VIEW/EDIT/FULL)
     */
    String getUserPermission(Long boardId, String username);

    /**
     * 수정 권한 확인
     *
     * @param boardId 보드 ID
     * @param username 사용자 Username
     * @return 수정 가능 여부
     */
    boolean canEdit(Long boardId, String username);

    /**
     * 삭제 권한 확인
     *
     * @param boardId 보드 ID
     * @param username 사용자 Username
     * @return 삭제 가능 여부
     */
    boolean canDelete(Long boardId, String username);

    /**
     * 보드 소유권 이전
     * - 보드를 다른 사용자에게 이관
     * - 보드명이 "보드이관"으로 자동 변경됨
     *
     * @param boardId   보드 ID
     * @param request   이전 요청
     * @param currentUsername 요청 사용자 Username
     * @return 이전된 보드 응답
     */
    BoardResponse transferBoardOwnership(Long boardId, BoardTransferRequest request, String currentUsername);

    // =============================================
    // v2.0: 보드 카테고리 관리
    // =============================================

    /**
     * 보드에 연결된 카테고리 목록 조회
     *
     * @param boardId 보드 ID
     * @return 보드 카테고리 목록
     */
    List<BoardCategoryResponse> getBoardCategories(Long boardId);

    /**
     * 보드에 카테고리 추가
     *
     * @param boardId    보드 ID
     * @param categoryId 카테고리 ID
     * @param createdBy  생성자 Username
     */
    void addBoardCategory(Long boardId, Long categoryId, String createdBy);

    /**
     * 보드에서 카테고리 제거
     *
     * @param boardId    보드 ID
     * @param categoryId 카테고리 ID
     */
    void removeBoardCategory(Long boardId, Long categoryId);

    /**
     * 보드의 기본 카테고리 설정
     *
     * @param boardId    보드 ID
     * @param categoryId 카테고리 ID
     * @param username   요청 사용자 Username
     */
    void setDefaultCategory(Long boardId, Long categoryId, String username);

    // =============================================
    // v2.0: 보드 속성 관리
    // =============================================

    /**
     * 보드에 선택된 속성 목록 조회
     *
     * @param boardId 보드 ID
     * @return 보드 속성 목록
     */
    List<BoardPropertyResponse> getBoardProperties(Long boardId);

    /**
     * 보드에 속성 추가
     *
     * @param boardId    보드 ID
     * @param propertyId 속성 ID
     * @param request    속성 설정 요청 (필수여부, 기본값 등)
     * @param createdBy  생성자 Username
     */
    void addBoardProperty(Long boardId, Long propertyId, BoardPropertyRequest request, String createdBy);

    /**
     * 보드에서 속성 제거
     *
     * @param boardId    보드 ID
     * @param propertyId 속성 ID
     */
    void removeBoardProperty(Long boardId, Long propertyId);

    /**
     * 보드 속성 설정 수정 (필수여부, 기본값 등)
     *
     * @param boardId    보드 ID
     * @param propertyId 속성 ID
     * @param request    수정 요청
     * @param updatedBy  수정자 Username
     */
    void updateBoardProperty(Long boardId, Long propertyId, BoardPropertyRequest request, String updatedBy);
}
