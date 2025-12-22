package com.taskflow.mapper;

import com.taskflow.domain.BoardShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 보드 공유 Mapper 인터페이스
 */
@Mapper
public interface BoardShareMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 공유 ID로 조회
     *
     * @param boardShareId 공유 ID
     * @return 공유 정보 (Optional)
     */
    Optional<BoardShare> findById(@Param("boardShareId") Long boardShareId);

    /**
     * 보드별 공유 사용자 목록 조회
     *
     * @param boardId 보드 ID
     * @return 공유 사용자 목록
     */
    List<BoardShare> findByBoardId(@Param("boardId") Long boardId);

    /**
     * 사용자별 공유받은 보드 목록 조회
     *
     * @param userId 사용자 ID
     * @return 공유받은 보드 목록
     */
    List<BoardShare> findByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 특정 보드에 공유되어 있는지 확인
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 공유 여부
     */
    boolean existsByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 특정 사용자의 특정 보드에 대한 공유 정보 조회
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 공유 정보 (Optional)
     */
    Optional<BoardShare> findByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 보드에 공유된 사용자가 있는지 확인
     *
     * @param boardId 보드 ID
     * @return 공유 사용자 존재 여부
     */
    boolean hasShares(@Param("boardId") Long boardId);

    /**
     * 보드 공유 사용자 수 조회
     *
     * @param boardId 보드 ID
     * @return 공유 사용자 수
     */
    int countByBoardId(@Param("boardId") Long boardId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 보드 공유 추가
     *
     * @param boardShare 공유 엔티티
     * @return 영향받은 행 수
     */
    int insert(BoardShare boardShare);

    /**
     * 보드 공유 권한 수정 (공유 ID 기준)
     *
     * @param boardShareId 공유 ID
     * @param permission   권한 레벨
     * @return 영향받은 행 수
     */
    int updatePermission(@Param("boardShareId") Long boardShareId, @Param("permission") String permission);

    /**
     * 보드 공유 권한 수정 (보드ID/사용자ID 기준)
     *
     * @param boardId    보드 ID
     * @param userId     사용자 ID
     * @param permission 권한 레벨
     * @param updatedBy  수정자 ID
     * @return 영향받은 행 수
     */
    int updatePermissionByBoardAndUser(@Param("boardId") Long boardId, @Param("userId") Long userId,
                                        @Param("permission") String permission, @Param("updatedBy") Long updatedBy);

    /**
     * 보드 공유 제거 (특정 사용자)
     *
     * @param boardId 보드 ID
     * @param userId  사용자 ID
     * @return 영향받은 행 수
     */
    int deleteByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

    /**
     * 보드의 모든 공유 제거
     *
     * @param boardId 보드 ID
     * @return 영향받은 행 수
     */
    int deleteByBoardId(@Param("boardId") Long boardId);

    /**
     * 사용자의 모든 공유 제거
     *
     * @param userId 사용자 ID
     * @return 영향받은 행 수
     */
    int deleteByUserId(@Param("userId") Long userId);
}
