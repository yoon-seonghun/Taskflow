package com.taskflow.service;

import com.taskflow.dto.group.*;

import java.util.List;

/**
 * 그룹 서비스 인터페이스
 */
public interface GroupService {

    // =============================================
    // 그룹 조회
    // =============================================

    /**
     * 그룹 ID로 조회
     *
     * @param groupId 그룹 ID
     * @return 그룹 응답
     */
    GroupResponse getGroup(Long groupId);

    /**
     * 전체 그룹 목록 조회
     *
     * @param useYn 사용 여부 필터 (null = 전체)
     * @return 그룹 목록
     */
    List<GroupResponse> getGroups(String useYn);

    // =============================================
    // 그룹 등록/수정/삭제
    // =============================================

    /**
     * 그룹 등록
     *
     * @param request   등록 요청
     * @param createdBy 생성자 ID
     * @return 생성된 그룹 응답
     */
    GroupResponse createGroup(GroupCreateRequest request, Long createdBy);

    /**
     * 그룹 수정
     *
     * @param groupId   그룹 ID
     * @param request   수정 요청
     * @param updatedBy 수정자 ID
     * @return 수정된 그룹 응답
     */
    GroupResponse updateGroup(Long groupId, GroupUpdateRequest request, Long updatedBy);

    /**
     * 그룹 순서 변경
     *
     * @param groupId   그룹 ID
     * @param request   순서 변경 요청
     * @param updatedBy 수정자 ID
     * @return 수정된 그룹 응답
     */
    GroupResponse updateGroupOrder(Long groupId, GroupOrderRequest request, Long updatedBy);

    /**
     * 그룹 삭제
     *
     * @param groupId 그룹 ID
     */
    void deleteGroup(Long groupId);

    // =============================================
    // 그룹 멤버 관리
    // =============================================

    /**
     * 그룹별 멤버 목록 조회
     *
     * @param groupId 그룹 ID
     * @return 멤버 목록
     */
    List<GroupMemberResponse> getGroupMembers(Long groupId);

    /**
     * 사용자별 소속 그룹 목록 조회
     *
     * @param userId 사용자 ID
     * @return 그룹 목록
     */
    List<GroupMemberResponse> getUserGroups(Long userId);

    /**
     * 그룹 멤버 추가
     *
     * @param groupId   그룹 ID
     * @param request   멤버 추가 요청
     * @param createdBy 생성자 ID
     * @return 멤버 정보
     */
    GroupMemberResponse addGroupMember(Long groupId, GroupMemberRequest request, Long createdBy);

    /**
     * 그룹 멤버 제거
     *
     * @param groupId 그룹 ID
     * @param userId  사용자 ID
     */
    void removeGroupMember(Long groupId, Long userId);

    // =============================================
    // 검증
    // =============================================

    /**
     * 그룹 코드 중복 확인
     *
     * @param groupCode 그룹 코드
     * @return 중복 여부
     */
    boolean existsByCode(String groupCode);
}
