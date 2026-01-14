package com.taskflow.service.impl;

import com.taskflow.domain.Item;
import com.taskflow.domain.ItemShare;
import com.taskflow.dto.assignment.AssignmentRequest;
import com.taskflow.dto.assignment.AssignmentResponse;
import com.taskflow.dto.item.ItemAccessInfo;
import com.taskflow.mapper.ItemMapper;
import com.taskflow.mapper.ItemShareMapper;
import com.taskflow.service.EmailService;
import com.taskflow.service.ItemAssignmentService;
import com.taskflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 업무 배정 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemAssignmentServiceImpl implements ItemAssignmentService {

    private final ItemMapper itemMapper;
    private final ItemShareMapper itemShareMapper;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Override
    @Transactional
    public AssignmentResponse assignItem(Long boardId, Long itemId, AssignmentRequest request, String currentUsername) {
        // 1. 업무 조회 및 소유자 확인
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("업무를 찾을 수 없습니다: " + itemId);
        }

        // 2. 소유자만 배정 가능
        if (!item.getCreatedBy().equals(currentUsername)) {
            throw new IllegalStateException("업무 소유자만 담당자를 배정할 수 있습니다.");
        }

        // 3. 자기 자신에게 배정 불가
        if (request.getAssigneeUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("자기 자신에게는 배정할 수 없습니다.");
        }

        // 4. 업무 소유자에게 배정 불가
        if (request.getAssigneeUsername().equals(item.getCreatedBy())) {
            throw new IllegalArgumentException("업무 소유자에게는 배정할 수 없습니다.");
        }

        // 5. 기존 공유 확인
        ItemShare existingShare = itemShareMapper.selectByItemIdAndUsername(itemId, request.getAssigneeUsername());

        ItemShare itemShare;
        if (existingShare != null) {
            // 기존 공유를 배정으로 업데이트
            itemShareMapper.updateAssignment(
                    itemId,
                    request.getAssigneeUsername(),
                    ItemShare.SHARE_TYPE_ASSIGN,
                    request.getPermissionLevel(),
                    currentUsername,
                    LocalDateTime.now(),
                    currentUsername
            );
            itemShare = itemShareMapper.selectByItemIdAndUsername(itemId, request.getAssigneeUsername());
        } else {
            // 새 배정 생성
            itemShare = ItemShare.builder()
                    .itemId(itemId)
                    .username(request.getAssigneeUsername())
                    .shareType(ItemShare.SHARE_TYPE_ASSIGN)
                    .permission(request.getPermissionLevel())
                    .assignedBy(currentUsername)
                    .assignedAt(LocalDateTime.now())
                    .createdBy(currentUsername)
                    .build();
            itemShareMapper.insert(itemShare);
            itemShare = itemShareMapper.selectByItemIdAndUsername(itemId, request.getAssigneeUsername());
        }

        // 6. 업무 담당자 업데이트
        itemMapper.updateAssignee(itemId, request.getAssigneeUsername(), currentUsername);

        // 7. 알림 발송
        boolean notificationSent = false;
        if (Boolean.TRUE.equals(request.getSendNotification())) {
            try {
                notificationService.sendItemAssignedNotification(
                        itemId,
                        item.getTitle(),
                        request.getAssigneeUsername(),
                        currentUsername,
                        String.valueOf(boardId)
                );
                notificationSent = true;
            } catch (Exception e) {
                log.error("알림 발송 실패", e);
            }
        }

        // 8. 이메일 발송
        boolean emailSent = false;
        if (Boolean.TRUE.equals(request.getSendEmail())) {
            try {
                emailService.sendTaskAssignNotification(
                        itemId,
                        request.getAssigneeUsername(),
                        currentUsername
                );
                emailSent = true;
            } catch (Exception e) {
                log.error("이메일 발송 실패", e);
            }
        }

        log.info("업무 배정 완료 - 업무ID: {}, 담당자: {}, 권한: {}", itemId, request.getAssigneeUsername(), request.getPermissionLevel());

        AssignmentResponse response = AssignmentResponse.from(itemShare);
        response.setEmailSent(emailSent);
        response.setNotificationSent(notificationSent);

        return response;
    }

    @Override
    @Transactional
    public void cancelAssignment(Long boardId, Long itemId, String assigneeUsername, String currentUsername) {
        // 1. 업무 조회 및 소유자 확인
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("업무를 찾을 수 없습니다: " + itemId);
        }

        // 2. 소유자만 취소 가능
        if (!item.getCreatedBy().equals(currentUsername)) {
            throw new IllegalStateException("업무 소유자만 배정을 취소할 수 있습니다.");
        }

        // 3. 공유 삭제
        itemShareMapper.delete(itemId, assigneeUsername);

        // 4. 담당자가 해당 사용자인 경우 담당자 초기화
        if (assigneeUsername.equals(item.getAssigneeUsername())) {
            itemMapper.updateAssignee(itemId, null, currentUsername);
        }

        log.info("업무 배정 취소 - 업무ID: {}, 담당자: {}", itemId, assigneeUsername);
    }

    @Override
    @Transactional
    public AssignmentResponse updateAssignmentPermission(Long boardId, Long itemId, String assigneeUsername,
                                                          String permissionLevel, String currentUsername) {
        // 1. 업무 조회 및 소유자 확인
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("업무를 찾을 수 없습니다: " + itemId);
        }

        // 2. 소유자만 권한 수정 가능
        if (!item.getCreatedBy().equals(currentUsername)) {
            throw new IllegalStateException("업무 소유자만 권한을 수정할 수 있습니다.");
        }

        // 3. 권한 업데이트
        itemShareMapper.updatePermission(itemId, assigneeUsername, permissionLevel, currentUsername);

        // 4. 업데이트된 정보 조회
        ItemShare itemShare = itemShareMapper.selectByItemIdAndUsername(itemId, assigneeUsername);

        log.info("배정 권한 수정 - 업무ID: {}, 담당자: {}, 권한: {}", itemId, assigneeUsername, permissionLevel);

        return AssignmentResponse.from(itemShare);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemAccessInfo getAccessInfo(Long boardId, Long itemId, String currentUsername) {
        // 1. 업무 조회
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            return ItemAccessInfo.noAccess();
        }

        // 2. 소유자인 경우
        if (currentUsername != null && currentUsername.equals(item.getCreatedBy())) {
            return ItemAccessInfo.forOwner();
        }

        // 3. 공유/배정 정보 조회
        ItemShare itemShare = itemShareMapper.selectByItemIdAndUsername(itemId, currentUsername);
        if (itemShare == null) {
            return ItemAccessInfo.noAccess();
        }

        // 4. 공유/배정 사용자 권한 정보 반환
        return ItemAccessInfo.forSharedUser(
                itemShare.getShareType(),
                itemShare.getPermission(),
                itemShare.getAssignedBy(),
                itemShare.getAssignedByName(),
                itemShare.getAssignedAt()
        );
    }
}
