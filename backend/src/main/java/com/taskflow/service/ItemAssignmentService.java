package com.taskflow.service;

import com.taskflow.dto.assignment.AssignmentRequest;
import com.taskflow.dto.assignment.AssignmentResponse;
import com.taskflow.dto.item.ItemAccessInfo;

/**
 * 업무 배정 서비스 인터페이스
 */
public interface ItemAssignmentService {

    /**
     * 업무 담당자 배정
     *
     * @param boardId 보드 ID
     * @param itemId 업무 ID
     * @param request 배정 요청
     * @param currentUsername 현재 사용자 USERNAME
     * @return 배정 결과
     */
    AssignmentResponse assignItem(Long boardId, Long itemId, AssignmentRequest request, String currentUsername);

    /**
     * 업무 배정 취소
     *
     * @param boardId 보드 ID
     * @param itemId 업무 ID
     * @param assigneeUsername 배정 대상 USERNAME
     * @param currentUsername 현재 사용자 USERNAME
     */
    void cancelAssignment(Long boardId, Long itemId, String assigneeUsername, String currentUsername);

    /**
     * 배정 권한 수정
     *
     * @param boardId 보드 ID
     * @param itemId 업무 ID
     * @param assigneeUsername 배정 대상 USERNAME
     * @param permissionLevel 새 권한 수준
     * @param currentUsername 현재 사용자 USERNAME
     * @return 수정된 배정 정보
     */
    AssignmentResponse updateAssignmentPermission(Long boardId, Long itemId, String assigneeUsername,
                                                   String permissionLevel, String currentUsername);

    /**
     * 현재 사용자의 업무 접근 권한 정보 조회
     *
     * @param boardId 보드 ID
     * @param itemId 업무 ID
     * @param currentUsername 현재 사용자 USERNAME
     * @return 접근 권한 정보
     */
    ItemAccessInfo getAccessInfo(Long boardId, Long itemId, String currentUsername);
}
