package com.taskflow.mapper;

import com.taskflow.domain.User;
import com.taskflow.dto.user.UserSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 Mapper 인터페이스
 */
@Mapper
public interface UserMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 사용자 ID로 조회
     */
    Optional<User> findById(@Param("userId") Long userId);

    /**
     * 로그인 아이디로 조회
     */
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * 사용자 목록 조회 (검색/필터/페이징)
     */
    List<User> findAll(@Param("request") UserSearchRequest request);

    /**
     * 사용자 총 개수 조회 (검색/필터)
     */
    long countAll(@Param("request") UserSearchRequest request);

    /**
     * 부서별 사용자 목록 조회
     */
    List<User> findByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 아이디 중복 확인
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 아이디 중복 확인 (자신 제외)
     */
    boolean existsByUsernameAndUserIdNot(
            @Param("username") String username,
            @Param("userId") Long userId
    );

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 사용자 등록
     */
    int insert(User user);

    /**
     * 사용자 정보 수정
     */
    int update(User user);

    /**
     * 비밀번호 변경
     */
    int updatePassword(
            @Param("userId") Long userId,
            @Param("password") String password,
            @Param("updatedBy") Long updatedBy
    );

    /**
     * 사용자 삭제 (물리 삭제)
     */
    int delete(@Param("userId") Long userId);

    /**
     * 사용자 비활성화 (논리 삭제)
     */
    int deactivate(
            @Param("userId") Long userId,
            @Param("updatedBy") Long updatedBy
    );

    /**
     * 마지막 로그인 일시 업데이트
     */
    int updateLastLoginAt(@Param("userId") Long userId);
}
