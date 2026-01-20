package com.taskflow.mapper.external;

import com.taskflow.domain.User;
import com.taskflow.dto.user.UserSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 외부 DB 사용자 Mapper 인터페이스
 *
 * 읽기 전용 - CRUD 불가
 * 외부 DB의 V_TASKFLOW_USER VIEW에서 데이터 조회
 */
@Mapper
public interface ExternalUserMapper {

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
    List<User> findByDepartmentCode(@Param("departmentCode") String departmentCode);

    /**
     * 아이디 중복 확인
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 전체 활성 사용자 목록 조회 (동기화용)
     */
    List<User> findAllActive();
}
