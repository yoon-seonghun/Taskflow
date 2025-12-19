package com.taskflow.mapper;

import com.taskflow.domain.Group;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 그룹 Mapper 인터페이스
 */
@Mapper
public interface GroupMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 그룹 ID로 조회
     *
     * @param groupId 그룹 ID
     * @return 그룹 (Optional)
     */
    Optional<Group> findById(@Param("groupId") Long groupId);

    /**
     * 그룹 코드로 조회
     *
     * @param groupCode 그룹 코드
     * @return 그룹 (Optional)
     */
    Optional<Group> findByCode(@Param("groupCode") String groupCode);

    /**
     * 전체 그룹 목록 조회
     *
     * @param useYn 사용 여부 필터 (null = 전체)
     * @return 그룹 목록
     */
    List<Group> findAll(@Param("useYn") String useYn);

    /**
     * 그룹 코드 중복 확인
     *
     * @param groupCode 그룹 코드
     * @return 중복 여부
     */
    boolean existsByCode(@Param("groupCode") String groupCode);

    /**
     * 그룹 코드 중복 확인 (자신 제외)
     *
     * @param groupCode 그룹 코드
     * @param groupId   제외할 그룹 ID
     * @return 중복 여부
     */
    boolean existsByCodeAndIdNot(@Param("groupCode") String groupCode, @Param("groupId") Long groupId);

    /**
     * 최대 정렬 순서 조회
     *
     * @return 최대 정렬 순서
     */
    Integer getMaxSortOrder();

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 그룹 등록
     *
     * @param group 그룹 엔티티
     * @return 영향받은 행 수
     */
    int insert(Group group);

    /**
     * 그룹 수정
     *
     * @param group 그룹 엔티티
     * @return 영향받은 행 수
     */
    int update(Group group);

    /**
     * 그룹 순서 변경
     *
     * @param groupId   그룹 ID
     * @param sortOrder 정렬 순서
     * @param updatedBy 수정자 ID
     * @return 영향받은 행 수
     */
    int updateOrder(@Param("groupId") Long groupId,
                    @Param("sortOrder") Integer sortOrder,
                    @Param("updatedBy") Long updatedBy);

    /**
     * 그룹 삭제 (물리 삭제)
     *
     * @param groupId 그룹 ID
     * @return 영향받은 행 수
     */
    int delete(@Param("groupId") Long groupId);

    /**
     * 그룹 비활성화 (논리 삭제)
     *
     * @param groupId   그룹 ID
     * @param updatedBy 수정자 ID
     * @return 영향받은 행 수
     */
    int deactivate(@Param("groupId") Long groupId, @Param("updatedBy") Long updatedBy);
}
