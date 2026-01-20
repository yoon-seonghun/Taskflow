package com.taskflow.mapper;

import com.taskflow.domain.UserGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자-그룹 매핑 Mapper 인터페이스
 */
@Mapper
public interface UserGroupMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 매핑 ID로 조회
     *
     * @param userGroupId 매핑 ID
     * @return 매핑 정보 (Optional)
     */
    Optional<UserGroup> findById(@Param("userGroupId") Long userGroupId);

    /**
     * 그룹별 멤버 목록 조회
     *
     * @param groupId 그룹 ID
     * @return 멤버 목록
     */
    List<UserGroup> findByGroupId(@Param("groupId") Long groupId);

    /**
     * 사용자별 소속 그룹 목록 조회
     *
     * @param username 사용자 USERNAME
     * @return 그룹 목록
     */
    List<UserGroup> findByUsername(@Param("username") String username);

    /**
     * 특정 사용자가 특정 그룹에 소속되어 있는지 확인
     *
     * @param username  사용자 USERNAME
     * @param groupId 그룹 ID
     * @return 소속 여부
     */
    boolean existsByUsernameAndGroupId(@Param("username") String username, @Param("groupId") Long groupId);

    /**
     * 그룹에 멤버가 있는지 확인
     *
     * @param groupId 그룹 ID
     * @return 멤버 존재 여부
     */
    boolean hasMembers(@Param("groupId") Long groupId);

    /**
     * 그룹 멤버 수 조회
     *
     * @param groupId 그룹 ID
     * @return 멤버 수
     */
    int countByGroupId(@Param("groupId") Long groupId);

    // =============================================
    // 등록/삭제
    // =============================================

    /**
     * 그룹 멤버 추가
     *
     * @param userGroup 매핑 엔티티
     * @return 영향받은 행 수
     */
    int insert(UserGroup userGroup);

    /**
     * 그룹 멤버 제거 (특정 사용자)
     *
     * @param groupId 그룹 ID
     * @param username  사용자 USERNAME
     * @return 영향받은 행 수
     */
    int deleteByGroupIdAndUsername(@Param("groupId") Long groupId, @Param("username") String username);

    /**
     * 그룹의 모든 멤버 제거
     *
     * @param groupId 그룹 ID
     * @return 영향받은 행 수
     */
    int deleteByGroupId(@Param("groupId") Long groupId);

    /**
     * 사용자의 모든 그룹 소속 제거
     *
     * @param username 사용자 USERNAME
     * @return 영향받은 행 수
     */
    int deleteByUsername(@Param("username") String username);
}
