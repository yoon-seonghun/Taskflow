package com.taskflow.mapper;

import com.taskflow.domain.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 보드 Mapper 인터페이스
 */
@Mapper
public interface BoardMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 보드 ID로 조회
     *
     * @param boardId 보드 ID
     * @return 보드 (Optional)
     */
    Optional<Board> findById(@Param("boardId") Long boardId);

    /**
     * 사용자가 소유한 보드 목록 조회
     *
     * @param ownerUsername 소유자 USERNAME
     * @param useYn   사용 여부 필터 (null = 전체)
     * @return 보드 목록
     */
    List<Board> findByOwnerUsername(@Param("ownerUsername") String ownerUsername, @Param("useYn") String useYn);

    /**
     * 사용자가 접근 가능한 모든 보드 목록 조회
     * (소유한 보드 + 공유받은 보드)
     *
     * @param username 사용자 USERNAME
     * @param useYn  사용 여부 필터 (null = 전체)
     * @return 보드 목록
     */
    List<Board> findAccessibleByUsername(@Param("username") String username, @Param("useYn") String useYn);

    /**
     * 전체 보드 목록 조회 (관리자용)
     *
     * @param useYn 사용 여부 필터 (null = 전체)
     * @return 보드 목록
     */
    List<Board> findAll(@Param("useYn") String useYn);

    /**
     * 사용자가 특정 보드에 접근 가능한지 확인
     * (소유자이거나 공유받은 경우)
     *
     * @param boardId 보드 ID
     * @param username  사용자 USERNAME
     * @return 접근 가능 여부
     */
    boolean hasAccess(@Param("boardId") Long boardId, @Param("username") String username);

    /**
     * 최대 정렬 순서 조회
     *
     * @param ownerUsername 소유자 USERNAME
     * @return 최대 정렬 순서
     */
    Integer getMaxSortOrder(@Param("ownerUsername") String ownerUsername);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 보드 등록
     *
     * @param board 보드 엔티티
     * @return 영향받은 행 수
     */
    int insert(Board board);

    /**
     * 보드 수정
     *
     * @param board 보드 엔티티
     * @return 영향받은 행 수
     */
    int update(Board board);

    /**
     * 보드 삭제 (물리 삭제)
     *
     * @param boardId 보드 ID
     * @return 영향받은 행 수
     */
    int delete(@Param("boardId") Long boardId);

    /**
     * 보드 비활성화 (논리 삭제)
     *
     * @param boardId   보드 ID
     * @param updatedBy 수정자 USERNAME
     * @return 영향받은 행 수
     */
    int deactivate(@Param("boardId") Long boardId, @Param("updatedBy") String updatedBy);

    /**
     * 보드에 아이템이 있는지 확인
     *
     * @param boardId 보드 ID
     * @return 아이템 존재 여부
     */
    boolean hasItems(@Param("boardId") Long boardId);

    // =============================================
    // 보드 관리 추가 메서드
    // =============================================

    /**
     * 소유한 보드 목록 조회 (정렬 순서 포함)
     *
     * @param username 사용자 USERNAME
     * @return 보드 목록
     */
    List<Board> findOwnedBoards(@Param("username") String username);

    /**
     * 공유받은 보드 목록 조회 (권한 정보 포함)
     *
     * @param username 사용자 USERNAME
     * @return 보드 목록 (Map 형태)
     */
    List<java.util.Map<String, Object>> findSharedBoards(@Param("username") String username);

    /**
     * 미완료 업무 수 조회
     *
     * @param boardId 보드 ID
     * @return 미완료 업무 수
     */
    int countPendingItems(@Param("boardId") Long boardId);

    /**
     * 미완료 업무 목록 조회
     *
     * @param boardId 보드 ID
     * @return 미완료 업무 목록
     */
    List<java.util.Map<String, Object>> findPendingItems(@Param("boardId") Long boardId);

    /**
     * 보드 순서 변경
     *
     * @param boardId   보드 ID
     * @param sortOrder 정렬 순서
     * @param updatedBy 수정자 USERNAME
     * @return 영향받은 행 수
     */
    int updateSortOrder(
            @Param("boardId") Long boardId,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 사용자의 보드 권한 조회
     *
     * @param boardId 보드 ID
     * @param username  사용자 USERNAME
     * @return 권한 (OWNER/VIEW/EDIT/FULL)
     */
    String getUserPermission(
            @Param("boardId") Long boardId,
            @Param("username") String username
    );

    /**
     * 소유자 USERNAME과 보드명으로 보드 조회
     * (업무 이관 시 "업무이관" 보드 자동 생성/조회용)
     *
     * @param ownerUsername   소유자 USERNAME
     * @param boardName 보드명
     * @return 보드 (Optional)
     */
    Optional<Board> findByOwnerUsernameAndName(
            @Param("ownerUsername") String ownerUsername,
            @Param("boardName") String boardName
    );

    /**
     * 보드 소유권 이전
     *
     * @param boardId     보드 ID
     * @param newOwnerUsername  새 소유자 USERNAME
     * @param newBoardName 새 보드명 (null이면 변경 안함)
     * @param updatedBy   수정자 USERNAME
     * @return 영향받은 행 수
     */
    int transferOwnership(
            @Param("boardId") Long boardId,
            @Param("newOwnerUsername") String newOwnerUsername,
            @Param("newBoardName") String newBoardName,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 특정 소유자의 보드 수 조회 (v2.2.1)
     *
     * @param ownerUsername 소유자 USERNAME
     * @return 보드 수
     */
    int countByOwnerUsername(@Param("ownerUsername") String ownerUsername);
}
