package com.taskflow.service;

import com.taskflow.dto.position.*;

import java.util.List;

/**
 * 직급 서비스 인터페이스
 *
 * 두 가지 모드 지원:
 * - internal: 기본 관리 모드 (CRUD 가능)
 * - external: 외부 DB 연동 모드 (조회만 가능)
 */
public interface PositionService {

    // =============================================
    // 모드 확인
    // =============================================

    /**
     * CRUD 활성화 여부 확인
     * - internal 모드: true (기본)
     * - external 모드: false
     *
     * @return CRUD 활성화 여부
     */
    boolean isCrudEnabled();

    // =============================================
    // 조회
    // =============================================

    /**
     * 직급 코드로 조회
     *
     * @param positionCode 직급 코드
     * @return 직급 응답
     */
    PositionResponse getPosition(String positionCode);

    /**
     * 전체 직급 목록 조회
     *
     * @param useYn 사용 여부 필터 (null = 전체)
     * @return 직급 목록
     */
    List<PositionResponse> getPositions(String useYn);

    /**
     * 직급 코드 중복 확인
     *
     * @param positionCode 직급 코드
     * @return 중복 여부
     */
    boolean existsByCode(String positionCode);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 직급 등록
     *
     * @param request 등록 요청
     * @param createdBy 생성자 Username
     * @return 생성된 직급 응답
     */
    PositionResponse createPosition(PositionCreateRequest request, String createdBy);

    /**
     * 직급 수정
     *
     * @param positionCode 직급 코드
     * @param request 수정 요청
     * @param updatedBy 수정자 Username
     * @return 수정된 직급 응답
     */
    PositionResponse updatePosition(String positionCode, PositionUpdateRequest request, String updatedBy);

    /**
     * 직급 순서 변경
     *
     * @param request 순서 변경 요청
     * @param updatedBy 수정자 Username
     */
    void updatePositionOrder(PositionOrderRequest request, String updatedBy);

    /**
     * 직급 삭제
     *
     * @param positionCode 직급 코드
     */
    void deletePosition(String positionCode);
}
