package com.taskflow.service;

import com.taskflow.domain.Item;
import com.taskflow.domain.ItemChecklist;
import com.taskflow.dto.checklist.ChecklistCreateRequest;
import com.taskflow.dto.checklist.ChecklistReorderRequest;
import com.taskflow.dto.checklist.ChecklistResponse;
import com.taskflow.dto.checklist.ChecklistUpdateRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.ItemChecklistMapper;
import com.taskflow.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 업무 내 체크리스트 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemChecklistService {

    private final ItemChecklistMapper checklistMapper;
    private final ItemMapper itemMapper;

    /**
     * 체크리스트 조회
     */
    @Transactional(readOnly = true)
    public ChecklistResponse getChecklist(Long checklistId) {
        ItemChecklist checklist = checklistMapper.selectById(checklistId);
        if (checklist == null) {
            throw new BusinessException("체크리스트를 찾을 수 없습니다.");
        }
        return ChecklistResponse.from(checklist);
    }

    /**
     * 업무별 체크리스트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChecklistResponse> getChecklists(Long itemId) {
        return checklistMapper.selectByItemId(itemId)
                .stream()
                .map(ChecklistResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 업무별 체크리스트 진행률 조회
     */
    @Transactional(readOnly = true)
    public int[] getProgress(Long itemId) {
        int total = checklistMapper.countByItemId(itemId);
        int completed = checklistMapper.countCompletedByItemId(itemId);
        return new int[] { completed, total };
    }

    /**
     * 담당자별 체크리스트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChecklistResponse> getChecklistsByAssignee(String assigneeUsername) {
        return checklistMapper.selectByAssigneeUsername(assigneeUsername)
                .stream()
                .map(ChecklistResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 체크리스트 생성
     */
    @Transactional
    public ChecklistResponse createChecklist(Long itemId, ChecklistCreateRequest request, String currentUsername) {
        // 업무 존재 확인
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> new BusinessException("업무를 찾을 수 없습니다."));

        // 다음 정렬 순서 조회
        Integer nextSortOrder = request.getSortOrder();
        if (nextSortOrder == null) {
            nextSortOrder = checklistMapper.getNextSortOrder(itemId);
        }

        ItemChecklist checklist = ItemChecklist.builder()
                .itemId(itemId)
                .title(request.getTitle())
                .isCompleted(false)
                .sortOrder(nextSortOrder)
                .dueDate(request.getDueDate())
                .assigneeUsername(request.getAssigneeUsername())
                .createdBy(currentUsername)
                .build();

        checklistMapper.insert(checklist);

        log.info("Checklist {} created for item {} by {}", checklist.getChecklistId(), itemId, currentUsername);

        return getChecklist(checklist.getChecklistId());
    }

    /**
     * 체크리스트 수정
     */
    @Transactional
    public ChecklistResponse updateChecklist(Long checklistId, ChecklistUpdateRequest request, String currentUsername) {
        ItemChecklist checklist = checklistMapper.selectById(checklistId);
        if (checklist == null) {
            throw new BusinessException("체크리스트를 찾을 수 없습니다.");
        }

        // 수정할 필드 업데이트
        if (request.getTitle() != null) {
            checklist.setTitle(request.getTitle());
        }
        if (request.getIsCompleted() != null) {
            checklist.setIsCompleted(request.getIsCompleted());
        }
        if (request.getSortOrder() != null) {
            checklist.setSortOrder(request.getSortOrder());
        }
        if (request.getDueDate() != null) {
            checklist.setDueDate(request.getDueDate());
        }
        if (request.getAssigneeUsername() != null) {
            checklist.setAssigneeUsername(request.getAssigneeUsername());
        }

        checklist.setUpdatedBy(currentUsername);

        checklistMapper.update(checklist);

        log.info("Checklist {} updated by {}", checklistId, currentUsername);

        return getChecklist(checklistId);
    }

    /**
     * 체크리스트 완료 상태 토글
     */
    @Transactional
    public ChecklistResponse toggleComplete(Long checklistId, String currentUsername) {
        ItemChecklist checklist = checklistMapper.selectById(checklistId);
        if (checklist == null) {
            throw new BusinessException("체크리스트를 찾을 수 없습니다.");
        }

        boolean newCompleted = !Boolean.TRUE.equals(checklist.getIsCompleted());
        checklistMapper.updateCompleted(checklistId, newCompleted, currentUsername);

        log.info("Checklist {} completion toggled to {} by {}", checklistId, newCompleted, currentUsername);

        return getChecklist(checklistId);
    }

    /**
     * 체크리스트 삭제
     */
    @Transactional
    public void deleteChecklist(Long checklistId, String currentUsername) {
        ItemChecklist checklist = checklistMapper.selectById(checklistId);
        if (checklist == null) {
            throw new BusinessException("체크리스트를 찾을 수 없습니다.");
        }

        checklistMapper.deleteById(checklistId);

        log.info("Checklist {} deleted by {}", checklistId, currentUsername);
    }

    /**
     * 체크리스트 순서 변경
     */
    @Transactional
    public void reorderChecklists(Long itemId, ChecklistReorderRequest request, String currentUsername) {
        List<Long> checklistIds = request.getChecklistIds();
        for (int i = 0; i < checklistIds.size(); i++) {
            checklistMapper.updateSortOrder(checklistIds.get(i), i, currentUsername);
        }

        log.info("Reordered {} checklists for item {} by {}", checklistIds.size(), itemId, currentUsername);
    }

    /**
     * 업무의 모든 체크리스트 삭제
     */
    @Transactional
    public void deleteAllChecklists(Long itemId) {
        checklistMapper.deleteByItemId(itemId);
        log.info("All checklists deleted for item {}", itemId);
    }
}
