package com.taskflow.service;

import com.taskflow.domain.Todo;
import com.taskflow.domain.TodoShare;
import com.taskflow.domain.User;
import com.taskflow.dto.todo.TodoShareRequest;
import com.taskflow.dto.todo.TodoShareResponse;
import com.taskflow.dto.todo.TodoShareUpdateRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.TodoMapper;
import com.taskflow.mapper.TodoShareMapper;
import com.taskflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Todo 공유 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoShareService {

    private final TodoShareMapper todoShareMapper;
    private final TodoMapper todoMapper;
    private final UserMapper userMapper;

    /**
     * Todo 공유 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TodoShareResponse> getShares(Long todoId) {
        return todoShareMapper.selectByTodoId(todoId)
                .stream()
                .map(TodoShareResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 공유받은 Todo 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TodoShareResponse> getSharedTodos(Long sharedUserId) {
        return todoShareMapper.selectBySharedUserId(sharedUserId)
                .stream()
                .map(TodoShareResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Todo 공유 추가
     */
    @Transactional
    public TodoShareResponse addShare(Long todoId, TodoShareRequest request, Long currentUserId, String currentUsername) {
        // Todo 존재 확인
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new BusinessException("Todo를 찾을 수 없습니다.");
        }

        // 소유자만 공유 가능
        if (!todo.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException("공유 권한이 없습니다.");
        }

        // 공유받을 사용자 조회 (username으로)
        User sharedUser = userMapper.findByUsername(request.getSharedUsername())
                .orElseThrow(() -> new BusinessException("공유받을 사용자를 찾을 수 없습니다."));

        // 자기 자신에게 공유 불가
        if (sharedUser.getUserId().equals(currentUserId)) {
            throw new BusinessException("자기 자신에게 공유할 수 없습니다.");
        }

        // 중복 확인
        if (todoShareMapper.existsByTodoIdAndUserId(todoId, sharedUser.getUserId())) {
            throw new BusinessException("이미 공유된 사용자입니다.");
        }

        TodoShare todoShare = TodoShare.builder()
                .todoId(todoId)
                .sharedUserId(sharedUser.getUserId())
                .sharedByUserId(currentUserId)
                .permission(request.getPermission() != null ? request.getPermission() : "VIEW")
                .createdBy(currentUsername)
                .build();

        todoShareMapper.insert(todoShare);

        log.info("Todo {} shared with user {} (permission: {}) by {}",
                todoId, request.getSharedUsername(), request.getPermission(), currentUsername);

        return TodoShareResponse.from(todoShareMapper.selectById(todoShare.getShareId()));
    }

    /**
     * Todo 공유 권한 변경
     */
    @Transactional
    public TodoShareResponse updatePermission(Long shareId, TodoShareUpdateRequest request, Long currentUserId) {
        TodoShare share = todoShareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException("공유 정보를 찾을 수 없습니다.");
        }

        // Todo 소유자만 권한 변경 가능
        Todo todo = todoMapper.selectById(share.getTodoId());
        if (!todo.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException("권한 변경 권한이 없습니다.");
        }

        todoShareMapper.updatePermission(shareId, request.getPermission());

        log.info("Todo share {} permission updated to {}", shareId, request.getPermission());

        return TodoShareResponse.from(todoShareMapper.selectById(shareId));
    }

    /**
     * Todo 공유 삭제
     */
    @Transactional
    public void deleteShare(Long shareId, Long currentUserId) {
        TodoShare share = todoShareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException("공유 정보를 찾을 수 없습니다.");
        }

        // Todo 소유자 또는 공유받은 본인만 삭제 가능
        Todo todo = todoMapper.selectById(share.getTodoId());
        if (!todo.getOwnerUserId().equals(currentUserId) && !share.getSharedUserId().equals(currentUserId)) {
            throw new BusinessException("삭제 권한이 없습니다.");
        }

        todoShareMapper.deleteById(shareId);

        log.info("Todo share {} deleted", shareId);
    }

    /**
     * 특정 사용자에 대한 Todo 공유 삭제
     */
    @Transactional
    public void deleteShareByUser(Long todoId, Long sharedUserId, Long currentUserId) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new BusinessException("Todo를 찾을 수 없습니다.");
        }

        // Todo 소유자 또는 공유받은 본인만 삭제 가능
        if (!todo.getOwnerUserId().equals(currentUserId) && !sharedUserId.equals(currentUserId)) {
            throw new BusinessException("삭제 권한이 없습니다.");
        }

        todoShareMapper.deleteByTodoIdAndUserId(todoId, sharedUserId);

        log.info("Todo {} share for user {} deleted", todoId, sharedUserId);
    }

    /**
     * 사용자의 Todo 접근 권한 확인
     */
    @Transactional(readOnly = true)
    public String getPermission(Long todoId, Long userId) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            return null;
        }

        // 소유자는 FULL 권한
        if (todo.getOwnerUserId().equals(userId)) {
            return "OWNER";
        }

        // 공유 권한 확인
        return todoShareMapper.getPermission(todoId, userId);
    }

    /**
     * Todo 공유 수 조회
     */
    @Transactional(readOnly = true)
    public int getShareCount(Long todoId) {
        return todoShareMapper.countByTodoId(todoId);
    }
}
