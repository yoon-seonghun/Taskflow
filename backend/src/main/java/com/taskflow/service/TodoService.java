package com.taskflow.service;

import com.taskflow.domain.Todo;
import com.taskflow.domain.TodoShare;
import com.taskflow.domain.User;
import com.taskflow.dto.todo.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.TodoMapper;
import com.taskflow.mapper.TodoShareMapper;
import com.taskflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 개인 Todo 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoMapper todoMapper;
    private final TodoShareMapper todoShareMapper;
    private final UserMapper userMapper;

    /**
     * Todo 조회
     */
    @Transactional(readOnly = true)
    public TodoResponse getTodo(Long todoId, Long currentUserId) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new BusinessException("Todo를 찾을 수 없습니다.");
        }

        // 접근 권한 확인 (소유자 또는 공유받은 사용자)
        if (!todo.getOwnerUserId().equals(currentUserId)) {
            String permission = todoShareMapper.getPermission(todoId, currentUserId);
            if (permission == null) {
                throw new BusinessException("접근 권한이 없습니다.");
            }
        }

        List<TodoShareResponse> shares = todoShareMapper.selectByTodoId(todoId)
                .stream()
                .map(TodoShareResponse::from)
                .collect(Collectors.toList());

        return TodoResponse.from(todo, shares);
    }

    /**
     * 사용자의 Todo 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TodoResponse> getTodos(Long ownerUserId, Boolean includeCompleted,
                                       LocalDate dueDateFrom, LocalDate dueDateTo) {
        return todoMapper.selectByOwnerUserId(ownerUserId, includeCompleted, dueDateFrom, dueDateTo)
                .stream()
                .map(TodoResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 오늘 마감 Todo 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TodoResponse> getTodayTodos(Long ownerUserId) {
        return todoMapper.selectTodayByOwnerUserId(ownerUserId)
                .stream()
                .map(TodoResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 지연 Todo 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TodoResponse> getOverdueTodos(Long ownerUserId) {
        return todoMapper.selectOverdueByOwnerUserId(ownerUserId)
                .stream()
                .map(TodoResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 완료된 Todo 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TodoResponse> getCompletedTodos(Long ownerUserId,
                                                LocalDate completedDateFrom, LocalDate completedDateTo) {
        return todoMapper.selectCompletedByOwnerUserId(ownerUserId, completedDateFrom, completedDateTo)
                .stream()
                .map(TodoResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 이관받은 Todo 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TodoResponse> getTransferredTodos(Long ownerUserId) {
        return todoMapper.selectTransferredByOwnerUserId(ownerUserId)
                .stream()
                .map(TodoResponse::from)
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
     * 사용자의 Todo 수 조회
     */
    @Transactional(readOnly = true)
    public int getTodoCount(Long ownerUserId) {
        return todoMapper.countByOwnerUserId(ownerUserId);
    }

    /**
     * 사용자의 활성 Todo 수 조회
     */
    @Transactional(readOnly = true)
    public int getActiveTodoCount(Long ownerUserId) {
        return todoMapper.countActiveByOwnerUserId(ownerUserId);
    }

    /**
     * Todo 생성
     */
    @Transactional
    public TodoResponse createTodo(Long ownerUserId, TodoCreateRequest request, String currentUsername) {
        Todo todo = Todo.builder()
                .ownerUserId(ownerUserId)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .dueDate(request.getDueDate())
                .dueTime(request.getDueTime())
                .isCompleted(false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .repeatType(request.getRepeatType())
                .repeatInterval(request.getRepeatInterval())
                .repeatDays(request.getRepeatDays())
                .repeatEndDate(request.getRepeatEndDate())
                .createdBy(currentUsername)
                .build();

        // 반복 설정 시 다음 마감일 계산
        if (request.getRepeatType() != null && !"NONE".equals(request.getRepeatType())) {
            todo.setNextDueDate(calculateNextDueDate(todo));
        }

        todoMapper.insert(todo);

        log.info("Todo {} created by user {}", todo.getTodoId(), currentUsername);

        return getTodo(todo.getTodoId(), ownerUserId);
    }

    /**
     * Todo 수정
     */
    @Transactional
    public TodoResponse updateTodo(Long todoId, TodoUpdateRequest request, Long currentUserId, String currentUsername) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new BusinessException("Todo를 찾을 수 없습니다.");
        }

        // 수정 권한 확인 (소유자 또는 EDIT 권한)
        if (!todo.getOwnerUserId().equals(currentUserId)) {
            String permission = todoShareMapper.getPermission(todoId, currentUserId);
            if (!"EDIT".equals(permission)) {
                throw new BusinessException("수정 권한이 없습니다.");
            }
        }

        // 수정할 필드 업데이트
        if (request.getTitle() != null) {
            todo.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            todo.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            todo.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            todo.setDueDate(request.getDueDate());
        }
        if (request.getDueTime() != null) {
            todo.setDueTime(request.getDueTime());
        }
        if (request.getIsCompleted() != null) {
            todo.setIsCompleted(request.getIsCompleted());
            if (request.getIsCompleted()) {
                todo.setCompletedAt(LocalDateTime.now());
            } else {
                todo.setCompletedAt(null);
            }
        }
        if (request.getSortOrder() != null) {
            todo.setSortOrder(request.getSortOrder());
        }
        if (request.getRepeatType() != null) {
            todo.setRepeatType(request.getRepeatType());
        }
        if (request.getRepeatInterval() != null) {
            todo.setRepeatInterval(request.getRepeatInterval());
        }
        if (request.getRepeatDays() != null) {
            todo.setRepeatDays(request.getRepeatDays());
        }
        if (request.getRepeatEndDate() != null) {
            todo.setRepeatEndDate(request.getRepeatEndDate());
        }

        todo.setUpdatedBy(currentUsername);

        todoMapper.update(todo);

        log.info("Todo {} updated by user {}", todoId, currentUsername);

        return getTodo(todoId, currentUserId);
    }

    /**
     * Todo 완료 상태 토글
     */
    @Transactional
    public TodoResponse toggleComplete(Long todoId, Long currentUserId, String currentUsername) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new BusinessException("Todo를 찾을 수 없습니다.");
        }

        // 수정 권한 확인
        if (!todo.getOwnerUserId().equals(currentUserId)) {
            String permission = todoShareMapper.getPermission(todoId, currentUserId);
            if (!"EDIT".equals(permission)) {
                throw new BusinessException("수정 권한이 없습니다.");
            }
        }

        boolean newCompleted = !Boolean.TRUE.equals(todo.getIsCompleted());
        todoMapper.updateCompleted(todoId, newCompleted, currentUsername);

        log.info("Todo {} completion toggled to {} by user {}", todoId, newCompleted, currentUsername);

        return getTodo(todoId, currentUserId);
    }

    /**
     * Todo 삭제
     */
    @Transactional
    public void deleteTodo(Long todoId, Long currentUserId, String currentUsername) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new BusinessException("Todo를 찾을 수 없습니다.");
        }

        // 소유자만 삭제 가능
        if (!todo.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException("삭제 권한이 없습니다.");
        }

        // 공유 먼저 삭제
        todoShareMapper.deleteByTodoId(todoId);

        // Todo 논리 삭제
        todoMapper.updateUseYn(todoId, "N", currentUsername);

        log.info("Todo {} deleted by user {}", todoId, currentUsername);
    }

    /**
     * Todo 이관
     */
    @Transactional
    public void transferTodo(Long todoId, TodoTransferRequest request, Long currentUserId, String currentUsername) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new BusinessException("Todo를 찾을 수 없습니다.");
        }

        // 소유자만 이관 가능
        if (!todo.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException("이관 권한이 없습니다.");
        }

        // 이관받을 사용자 조회 (username으로)
        User toUser = userMapper.findByUsername(request.getToUsername())
                .orElseThrow(() -> new BusinessException("이관받을 사용자를 찾을 수 없습니다."));

        // 자기 자신에게 이관 불가
        if (toUser.getUserId().equals(currentUserId)) {
            throw new BusinessException("자기 자신에게 이관할 수 없습니다.");
        }

        // 이관 대상 사용자가 공유 목록에 있으면 공유 해제
        if (todoShareMapper.existsByTodoIdAndUserId(todoId, toUser.getUserId())) {
            todoShareMapper.deleteByTodoIdAndUserId(todoId, toUser.getUserId());
        }

        todoMapper.updateTransfer(todoId, toUser.getUserId(), currentUserId, currentUsername);

        log.info("Todo {} transferred from user {} to user {} by {}",
                todoId, currentUserId, request.getToUsername(), currentUsername);
    }

    /**
     * 사용자의 Todo 일괄 이관 (사용자 삭제 시)
     */
    @Transactional
    public int transferAllTodos(TodoBulkTransferRequest request, String currentUsername) {
        // 이관 대상 사용자에게 공유된 것 먼저 삭제
        todoShareMapper.deleteBySharedUserId(request.getToUserId());

        // 일괄 이관
        int count = todoMapper.updateTransferByOwnerUserId(
                request.getFromUserId(),
                request.getToUserId(),
                currentUsername
        );

        log.info("Transferred {} todos from user {} to user {} by {}",
                count, request.getFromUserId(), request.getToUserId(), currentUsername);

        return count;
    }

    /**
     * 사용자의 Todo 일괄 삭제 (사용자 삭제 시)
     */
    @Transactional
    public int deleteAllTodos(Long userId, String currentUsername) {
        // 관련 공유 먼저 삭제
        todoShareMapper.deleteBySharedUserId(userId);

        // 일괄 삭제
        int count = todoMapper.updateUseYnByOwnerUserId(userId, "N", currentUsername);

        log.info("Deleted {} todos for user {} by {}", count, userId, currentUsername);

        return count;
    }

    /**
     * Todo 순서 변경
     */
    @Transactional
    public void reorderTodos(Long ownerUserId, TodoReorderRequest request, String currentUsername) {
        List<Long> todoIds = request.getTodoIds();
        for (int i = 0; i < todoIds.size(); i++) {
            todoMapper.updateSortOrder(todoIds.get(i), i, currentUsername);
        }

        log.info("Reordered {} todos for user {} by {}", todoIds.size(), ownerUserId, currentUsername);
    }

    /**
     * 다음 반복 마감일 계산
     */
    private LocalDate calculateNextDueDate(Todo todo) {
        LocalDate baseDate = todo.getDueDate() != null ? todo.getDueDate() : LocalDate.now();
        int interval = todo.getRepeatInterval() != null ? todo.getRepeatInterval() : 1;

        switch (todo.getRepeatType()) {
            case "DAILY":
                return baseDate.plusDays(interval);
            case "WEEKLY":
                return baseDate.plusWeeks(interval);
            case "MONTHLY":
                return baseDate.plusMonths(interval);
            case "YEARLY":
                return baseDate.plusYears(interval);
            default:
                return null;
        }
    }
}
