package com.taskflow.service.impl;

import com.taskflow.domain.Group;
import com.taskflow.domain.UserGroup;
import com.taskflow.dto.group.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.GroupMapper;
import com.taskflow.mapper.UserGroupMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 그룹 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupServiceImpl implements GroupService {

    private final GroupMapper groupMapper;
    private final UserGroupMapper userGroupMapper;
    private final UserMapper userMapper;

    // =============================================
    // 그룹 조회
    // =============================================

    @Override
    public GroupResponse getGroup(Long groupId) {
        Group group = groupMapper.findById(groupId)
                .orElseThrow(() -> BusinessException.groupNotFound(groupId));
        return GroupResponse.from(group);
    }

    @Override
    public List<GroupResponse> getGroups(String useYn) {
        List<Group> groups = groupMapper.findAll(useYn);
        return GroupResponse.fromList(groups);
    }

    // =============================================
    // 그룹 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public GroupResponse createGroup(GroupCreateRequest request, Long createdBy) {
        log.info("Creating group: code={}", request.getGroupCode());

        // 그룹 코드 중복 확인
        if (groupMapper.existsByCode(request.getGroupCode())) {
            throw BusinessException.duplicateCode(request.getGroupCode());
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = groupMapper.getMaxSortOrder() + 1;
        }

        // 그룹 엔티티 생성
        Group group = Group.builder()
                .groupCode(request.getGroupCode())
                .groupName(request.getGroupName())
                .description(request.getDescription())
                .color(request.getColor())
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        // 저장
        groupMapper.insert(group);
        log.info("Group created: id={}, code={}", group.getGroupId(), group.getGroupCode());

        return getGroup(group.getGroupId());
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long groupId, GroupUpdateRequest request, Long updatedBy) {
        log.info("Updating group: id={}", groupId);

        // 그룹 존재 확인
        Group group = groupMapper.findById(groupId)
                .orElseThrow(() -> BusinessException.groupNotFound(groupId));

        // 수정
        group.setGroupName(request.getGroupName());
        group.setDescription(request.getDescription());
        group.setColor(request.getColor());
        if (request.getSortOrder() != null) {
            group.setSortOrder(request.getSortOrder());
        }
        if (request.getUseYn() != null) {
            group.setUseYn(request.getUseYn());
        }
        group.setUpdatedBy(updatedBy);

        groupMapper.update(group);
        log.info("Group updated: id={}", groupId);

        return getGroup(groupId);
    }

    @Override
    @Transactional
    public GroupResponse updateGroupOrder(Long groupId, GroupOrderRequest request, Long updatedBy) {
        log.info("Updating group order: id={}, newOrder={}", groupId, request.getSortOrder());

        // 그룹 존재 확인
        groupMapper.findById(groupId)
                .orElseThrow(() -> BusinessException.groupNotFound(groupId));

        // 순서 변경
        groupMapper.updateOrder(groupId, request.getSortOrder(), updatedBy);
        log.info("Group order updated: id={}", groupId);

        return getGroup(groupId);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        log.info("Deleting group: id={}", groupId);

        // 그룹 존재 확인
        groupMapper.findById(groupId)
                .orElseThrow(() -> BusinessException.groupNotFound(groupId));

        // 그룹 멤버 존재 확인
        if (userGroupMapper.hasMembers(groupId)) {
            throw BusinessException.dataInUse("그룹에 소속된 멤버가 존재하여 삭제할 수 없습니다. 멤버를 먼저 제거해주세요.");
        }

        // 삭제
        groupMapper.delete(groupId);
        log.info("Group deleted: id={}", groupId);
    }

    // =============================================
    // 그룹 멤버 관리
    // =============================================

    @Override
    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        // 그룹 존재 확인
        groupMapper.findById(groupId)
                .orElseThrow(() -> BusinessException.groupNotFound(groupId));

        List<UserGroup> members = userGroupMapper.findByGroupId(groupId);
        return GroupMemberResponse.fromList(members);
    }

    @Override
    public List<GroupMemberResponse> getUserGroups(Long userId) {
        // 사용자 존재 확인
        userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        List<UserGroup> userGroups = userGroupMapper.findByUserId(userId);
        return GroupMemberResponse.fromList(userGroups);
    }

    @Override
    @Transactional
    public GroupMemberResponse addGroupMember(Long groupId, GroupMemberRequest request, Long createdBy) {
        log.info("Adding group member: groupId={}, userId={}", groupId, request.getUserId());

        // 그룹 존재 확인
        groupMapper.findById(groupId)
                .orElseThrow(() -> BusinessException.groupNotFound(groupId));

        // 사용자 존재 확인
        userMapper.findById(request.getUserId())
                .orElseThrow(() -> BusinessException.userNotFound(request.getUserId()));

        // 이미 멤버인지 확인
        if (userGroupMapper.existsByUserIdAndGroupId(request.getUserId(), groupId)) {
            throw BusinessException.conflict("이미 해당 그룹의 멤버입니다.");
        }

        // 멤버 추가
        UserGroup userGroup = UserGroup.builder()
                .userId(request.getUserId())
                .groupId(groupId)
                .createdBy(createdBy)
                .build();

        userGroupMapper.insert(userGroup);
        log.info("Group member added: userGroupId={}", userGroup.getUserGroupId());

        // 저장된 정보 조회하여 반환
        return GroupMemberResponse.from(
                userGroupMapper.findById(userGroup.getUserGroupId()).orElse(null)
        );
    }

    @Override
    @Transactional
    public void removeGroupMember(Long groupId, Long userId) {
        log.info("Removing group member: groupId={}, userId={}", groupId, userId);

        // 그룹 존재 확인
        groupMapper.findById(groupId)
                .orElseThrow(() -> BusinessException.groupNotFound(groupId));

        // 멤버인지 확인
        if (!userGroupMapper.existsByUserIdAndGroupId(userId, groupId)) {
            throw BusinessException.notFound("해당 사용자가 그룹의 멤버가 아닙니다.");
        }

        // 멤버 제거
        userGroupMapper.deleteByGroupIdAndUserId(groupId, userId);
        log.info("Group member removed: groupId={}, userId={}", groupId, userId);
    }

    // =============================================
    // 검증
    // =============================================

    @Override
    public boolean existsByCode(String groupCode) {
        return groupMapper.existsByCode(groupCode);
    }
}
