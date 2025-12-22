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
     * @param ownerId 소유자 ID
     * @param useYn   사용 여부 필터 (null = 전체)
     * @return 보드 목록
     */
    List<Board> findByOwnerId(@Param("ownerId") Long ownerId, @Param("useYn") String useYn);

    /**
     * 사용자가 접근 가능한 모든 보드 목록 조회
     * (소유한 보드 + 공유받은 보드)
     *
     * @param userId 사용자 ID
     * @param useYn  사용 여부 필터 (null = 전체)
     * @return 보드 목록
     */
    List<Board> findAccessibleByUserId(@Param("userId") Long userId, @Param("useYn") String useYn);

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
     * @param userId  사용자 ID
     * @return 접근 가능 여부
     */
    boolean hasAccess(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 최대 정렬 순서 조회
     *
     * @param ownerId 소유자 ID
     * @return 최대 정렬 순서
     */
    Integer getMaxSortOrder(@Param("ownerId") Long ownerId);

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
     * @param updatedBy 수정자 ID
     * @return 영향받은 행 수
     */
    int deactivate(@Param("boardId") Long boardId, @Param("updatedBy") Long updatedBy);

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
     * @param userId 사용자 ID
     * @return 보드 목록
     */
    List<Board> findOwnedBoards(@Param("userId") Long userId);

    /**
     * 공유받은 보드 목록 조회 (권한 정보 포함)
     *
     * @param userId 사용자 ID
     * @return 보드 목록 (Map 형태)
     */
    List<java.util.Map<String, Object>> findSharedBoards(@Param("userId") Long userId);

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
     * @param updatedBy 수정자 ID
     * @return 영향받은 행 수
     */
    int updateSortOrder(
            @Param("boardId") Long boardId,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") Long updatedBy
    );

    /**
     * 사용자의 보드 권한 조회
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 권한 (OWNER/VIEW/EDIT/FULL)
     */
    String getUserPermission(
            @Param("boardId") Long boardId,
            @Param("userId") Long userId
    );
}
