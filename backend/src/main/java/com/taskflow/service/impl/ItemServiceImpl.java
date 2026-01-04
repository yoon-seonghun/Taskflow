package com.taskflow.service.impl;

import com.taskflow.domain.*;
import com.taskflow.dto.item.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.*;
import com.taskflow.service.ItemService;
import com.taskflow.sse.SseEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 아이템 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final ItemPropertyMapper itemPropertyMapper;
    private final BoardMapper boardMapper;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final PropertyDefMapper propertyDefMapper;
    private final SseEventPublisher sseEventPublisher;

    // =============================================
    // 아이템 조회
    // =============================================

    @Override
    public ItemResponse getItem(Long itemId) {
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        // 동적 속성값 로드
        loadItemProperties(item);

        return ItemResponse.from(item);
    }

    @Override
    public ItemPageResponse getItemsByBoardId(Long boardId, ItemSearchRequest request) {
        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 아이템 목록 조회
        List<Item> items = itemMapper.findByBoardIdWithFilter(boardId, request);

        // 동적 속성값 로드
        loadItemPropertiesBatch(items);

        // 총 개수 조회
        long totalElements = itemMapper.countByBoardIdWithFilter(boardId, request);

        // 응답 변환
        List<ItemResponse> content = ItemResponse.fromList(items);

        return ItemPageResponse.of(content, request.getPage(), request.getSize(), totalElements);
    }

    @Override
    public List<ItemResponse> getActiveItemsByBoardId(Long boardId) {
        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        List<Item> items = itemMapper.findActiveByBoardId(boardId);
        loadItemPropertiesBatch(items);

        return ItemResponse.fromList(items);
    }

    @Override
    public List<ItemResponse> getTodayCompletedOrDeleted(Long boardId) {
        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        List<Item> items = itemMapper.findTodayCompletedOrDeleted(boardId);
        return ItemResponse.fromList(items);
    }

    @Override
    public List<ItemResponse> getItemsByGroupId(Long groupId) {
        // 그룹 존재 확인
        groupMapper.findById(groupId)
                .orElseThrow(() -> BusinessException.groupNotFound(groupId));

        List<Item> items = itemMapper.findByGroupId(groupId);
        loadItemPropertiesBatch(items);

        return ItemResponse.fromList(items);
    }

    @Override
    public List<ItemResponse> getItemsByAssigneeUsername(String assigneeUsername, Long boardId) {
        // 사용자 존재 확인
        userMapper.findByUsername(assigneeUsername)
                .orElseThrow(() -> BusinessException.userNotFound(assigneeUsername));

        List<Item> items = itemMapper.findByAssigneeUsername(assigneeUsername, boardId);
        loadItemPropertiesBatch(items);

        return ItemResponse.fromList(items);
    }

    // =============================================
    // 아이템 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public ItemResponse createItem(Long boardId, ItemCreateRequest request, String createdBy) {
        log.info("Creating item: boardId={}, title={}", boardId, request.getTitle());

        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 그룹 존재 확인
        if (request.getGroupId() != null) {
            groupMapper.findById(request.getGroupId())
                    .orElseThrow(() -> BusinessException.groupNotFound(request.getGroupId()));
        }

        // 담당자 존재 확인
        if (request.getAssigneeUsername() != null) {
            userMapper.findByUsername(request.getAssigneeUsername())
                    .orElseThrow(() -> BusinessException.userNotFound(request.getAssigneeUsername()));
        }

        // 기본값 설정
        String status = request.getStatus() != null ? request.getStatus() : Item.STATUS_NOT_STARTED;
        String priority = request.getPriority() != null ? request.getPriority() : Item.PRIORITY_NORMAL;

        // 아이템 엔티티 생성
        Item item = Item.builder()
                .boardId(boardId)
                .groupId(request.getGroupId())
                .categoryId(request.getCategoryId())
                .title(request.getTitle())
                .content(request.getContent())
                .description(request.getDescription())
                .status(status)
                .priority(priority)
                .assigneeUsername(request.getAssigneeUsername())
                .requestDate(request.getRequestDate())
                .dueDate(request.getDueDate())
                .createdBy(createdBy)
                .build();

        // 아이템 저장
        itemMapper.insert(item);
        log.info("Item created: id={}", item.getItemId());

        // 동적 속성값 저장
        if (request.getProperties() != null && !request.getProperties().isEmpty()) {
            saveItemProperties(item.getItemId(), boardId, request.getProperties(), createdBy);
        }

        ItemResponse response = getItem(item.getItemId());

        // SSE 이벤트 발행
        sseEventPublisher.publishItemCreated(boardId, response, createdBy);

        return response;
    }

    @Override
    @Transactional
    public ItemResponse updateItem(Long itemId, ItemUpdateRequest request, String updatedBy) {
        log.info("Updating item: id={}", itemId);

        // 아이템 존재 확인
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        // 그룹 존재 확인
        if (request.getGroupId() != null) {
            groupMapper.findById(request.getGroupId())
                    .orElseThrow(() -> BusinessException.groupNotFound(request.getGroupId()));
        }

        // 담당자 존재 확인
        if (request.getAssigneeUsername() != null) {
            userMapper.findByUsername(request.getAssigneeUsername())
                    .orElseThrow(() -> BusinessException.userNotFound(request.getAssigneeUsername()));
        }

        // clearFields 지원 - 명시적으로 null로 설정할 필드 목록
        java.util.Set<String> clearFields = request.getClearFields() != null
                ? request.getClearFields()
                : java.util.Collections.emptySet();

        // 수정 - null이 아닌 필드만 업데이트 (partial update 지원)
        if (request.getTitle() != null) {
            item.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            item.setContent(request.getContent());
        }
        // description은 명시적으로 전송된 경우에만 업데이트 (빈 문자열 허용)
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        // groupId - clearFields에 포함되면 null 설정
        if (clearFields.contains("groupId")) {
            item.setGroupId(null);
        } else if (request.getGroupId() != null) {
            item.setGroupId(request.getGroupId());
        }
        // categoryId - clearFields에 포함되면 null 설정
        if (clearFields.contains("categoryId")) {
            item.setCategoryId(null);
        } else if (request.getCategoryId() != null) {
            item.setCategoryId(request.getCategoryId());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            item.setPriority(request.getPriority());
        }
        // assigneeUsername - clearFields에 포함되면 null 설정
        if (clearFields.contains("assigneeUsername")) {
            item.setAssigneeUsername(null);
        } else if (request.getAssigneeUsername() != null) {
            item.setAssigneeUsername(request.getAssigneeUsername());
        }
        // requestDate - clearFields에 포함되면 null 설정
        if (clearFields.contains("requestDate")) {
            item.setRequestDate(null);
        } else if (request.getRequestDate() != null) {
            item.setRequestDate(request.getRequestDate());
        }
        // dueDate - clearFields에 포함되면 null 설정
        if (clearFields.contains("dueDate")) {
            item.setDueDate(null);
        } else if (request.getDueDate() != null) {
            item.setDueDate(request.getDueDate());
        }
        item.setUpdatedBy(updatedBy);

        itemMapper.update(item);
        log.info("Item updated: id={}", itemId);

        // 동적 속성값 저장
        if (request.getProperties() != null) {
            saveItemProperties(itemId, item.getBoardId(), request.getProperties(), updatedBy);
        }

        ItemResponse response = getItem(itemId);

        // SSE 이벤트 발행
        sseEventPublisher.publishItemUpdated(item.getBoardId(), response, updatedBy);

        return response;
    }

    @Override
    @Transactional
    public ItemResponse completeItem(Long itemId, String completedBy) {
        log.info("Completing item: id={}", itemId);

        // 아이템 존재 확인
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        // 이미 완료/삭제된 경우
        if (item.isCompleted()) {
            throw BusinessException.badRequest("이미 완료된 아이템입니다.");
        }
        if (item.isDeleted()) {
            throw BusinessException.badRequest("삭제된 아이템은 완료할 수 없습니다.");
        }

        itemMapper.complete(itemId, completedBy);
        log.info("Item completed: id={}", itemId);

        ItemResponse response = getItem(itemId);

        // SSE 이벤트 발행
        sseEventPublisher.publishItemCompleted(item.getBoardId(), response, completedBy);

        return response;
    }

    @Override
    @Transactional
    public ItemResponse deleteItem(Long itemId, String deletedBy) {
        log.info("Deleting item: id={}", itemId);

        // 아이템 존재 확인
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        // 이미 삭제된 경우
        if (item.isDeleted()) {
            throw BusinessException.badRequest("이미 삭제된 아이템입니다.");
        }

        itemMapper.softDelete(itemId, deletedBy);
        log.info("Item deleted: id={}", itemId);

        ItemResponse response = getItem(itemId);

        // SSE 이벤트 발행
        sseEventPublisher.publishItemDeleted(item.getBoardId(), response, deletedBy);

        return response;
    }

    @Override
    @Transactional
    public ItemResponse restoreItem(Long itemId, String updatedBy) {
        log.info("Restoring item: id={}", itemId);

        // 아이템 존재 확인
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        // 완료/삭제 상태가 아닌 경우
        if (!item.isCompleted() && !item.isDeleted()) {
            throw BusinessException.badRequest("완료되거나 삭제된 아이템만 복원할 수 있습니다.");
        }

        itemMapper.restore(itemId, updatedBy);
        log.info("Item restored: id={}", itemId);

        ItemResponse response = getItem(itemId);

        // SSE 이벤트 발행
        sseEventPublisher.publishItemRestored(item.getBoardId(), response, updatedBy);

        return response;
    }

    @Override
    @Transactional
    public void hardDeleteItem(Long itemId) {
        log.info("Hard deleting item: id={}", itemId);

        // 아이템 존재 확인
        itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        // 속성값 삭제
        itemPropertyMapper.deleteMultiByItemId(itemId);
        itemPropertyMapper.deleteByItemId(itemId);

        // 아이템 삭제
        itemMapper.delete(itemId);
        log.info("Item hard deleted: id={}", itemId);
    }

    // =============================================
    // Cross-board 조회
    // =============================================

    @Override
    public ItemPageResponse getOverdueItems(String username, CrossBoardSearchRequest request) {
        log.debug("Get overdue items: username={}, request={}", username, request);

        // 지연 아이템 목록 조회
        List<Item> items = itemMapper.findOverdueItems(username, request);

        // 동적 속성값 로드
        loadItemPropertiesBatch(items);

        // 총 개수 조회
        long totalElements = itemMapper.countOverdueItems(username, request);

        // 응답 변환
        List<ItemResponse> content = ItemResponse.fromList(items);

        return ItemPageResponse.of(content, request.getPage(), request.getSize(), totalElements);
    }

    @Override
    public ItemPageResponse getPendingItems(String username, CrossBoardSearchRequest request) {
        log.debug("Get pending items: username={}, request={}", username, request);

        // 보류 아이템 목록 조회
        List<Item> items = itemMapper.findPendingItems(username, request);

        // 동적 속성값 로드
        loadItemPropertiesBatch(items);

        // 총 개수 조회
        long totalElements = itemMapper.countPendingItems(username, request);

        // 응답 변환
        List<ItemResponse> content = ItemResponse.fromList(items);

        return ItemPageResponse.of(content, request.getPage(), request.getSize(), totalElements);
    }

    @Override
    public ItemPageResponse getActiveItemsCrossBoard(String username, CrossBoardSearchRequest request) {
        log.debug("Get active items cross-board: username={}, request={}", username, request);

        // 활성 아이템 목록 조회
        List<Item> items = itemMapper.findActiveItemsCrossBoard(username, request);

        // 동적 속성값 로드
        loadItemPropertiesBatch(items);

        // 총 개수 조회
        long totalElements = itemMapper.countActiveItemsCrossBoard(username, request);

        // 응답 변환
        List<ItemResponse> content = ItemResponse.fromList(items);

        return ItemPageResponse.of(content, request.getPage(), request.getSize(), totalElements);
    }

    @Override
    public Map<String, Object> getCrossBoardStats(String username) {
        log.debug("Get cross-board stats: username={}", username);

        CrossBoardSearchRequest emptyRequest = new CrossBoardSearchRequest();
        emptyRequest.setPage(0);
        emptyRequest.setSize(1); // 카운트만 필요하므로 1개만

        // 지연 업무 개수
        long overdueCount = itemMapper.countOverdueItems(username, emptyRequest);

        // 보류 업무 개수
        long pendingCount = itemMapper.countPendingItems(username, emptyRequest);

        // 전체 활성 업무 개수
        long activeCount = itemMapper.countActiveItemsCrossBoard(username, emptyRequest);

        // 우선순위별 지연 업무 개수
        CrossBoardSearchRequest urgentRequest = new CrossBoardSearchRequest();
        urgentRequest.setPriority("URGENT");
        long urgentOverdueCount = itemMapper.countOverdueItems(username, urgentRequest);

        CrossBoardSearchRequest highRequest = new CrossBoardSearchRequest();
        highRequest.setPriority("HIGH");
        long highOverdueCount = itemMapper.countOverdueItems(username, highRequest);

        Map<String, Object> stats = new HashMap<>();
        stats.put("overdueCount", overdueCount);
        stats.put("pendingCount", pendingCount);
        stats.put("activeCount", activeCount);
        stats.put("urgentOverdueCount", urgentOverdueCount);
        stats.put("highOverdueCount", highOverdueCount);

        return stats;
    }

    // =============================================
    // Private Methods
    // =============================================

    /**
     * 아이템의 동적 속성값 로드 (단일 아이템)
     */
    private void loadItemProperties(Item item) {
        List<ItemProperty> properties = itemPropertyMapper.findByItemId(item.getItemId());
        item.setProperties(properties);
    }

    /**
     * 여러 아이템의 동적 속성값 일괄 로드 (N+1 쿼리 최적화)
     */
    private void loadItemPropertiesBatch(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // 아이템 ID 목록 추출
        List<Long> itemIds = items.stream()
                .map(Item::getItemId)
                .collect(java.util.stream.Collectors.toList());

        // 일괄 조회
        List<ItemProperty> allProperties = itemPropertyMapper.findByItemIds(itemIds);

        // 아이템 ID별로 그룹화
        java.util.Map<Long, List<ItemProperty>> propertyMap = allProperties.stream()
                .collect(java.util.stream.Collectors.groupingBy(ItemProperty::getItemId));

        // 각 아이템에 속성값 설정
        for (Item item : items) {
            List<ItemProperty> props = propertyMap.getOrDefault(item.getItemId(), java.util.Collections.emptyList());
            item.setProperties(props);
        }
    }

    /**
     * 아이템의 동적 속성값 저장
     */
    private void saveItemProperties(Long itemId, Long boardId, Map<Long, Object> propertyValues, String username) {
        for (Map.Entry<Long, Object> entry : propertyValues.entrySet()) {
            Long propertyId = entry.getKey();
            Object value = entry.getValue();

            // 속성 정의 조회
            PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                    .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

            // 보드 일치 확인
            if (!boardId.equals(propertyDef.getBoardId())) {
                throw BusinessException.badRequest("속성이 해당 보드에 속하지 않습니다: " + propertyId);
            }

            // 값이 null이면 삭제
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                itemPropertyMapper.deleteByItemIdAndPropertyId(itemId, propertyId);
                if (PropertyDef.TYPE_MULTI_SELECT.equals(propertyDef.getPropertyType())) {
                    itemPropertyMapper.deleteMultiByItemIdAndPropertyId(itemId, propertyId);
                }
                continue;
            }

            // 다중선택의 경우 특별 처리
            if (PropertyDef.TYPE_MULTI_SELECT.equals(propertyDef.getPropertyType())) {
                saveMultiSelectProperty(itemId, propertyId, value, username);
            } else {
                saveSingleProperty(itemId, propertyId, propertyDef.getPropertyType(), value, username);
            }
        }
    }

    /**
     * 단일 값 속성 저장
     */
    private void saveSingleProperty(Long itemId, Long propertyId, String propertyType, Object value, String username) {
        ItemProperty itemProperty = ItemProperty.builder()
                .itemId(itemId)
                .propertyId(propertyId)
                .createdBy(username)
                .updatedBy(username)
                .build();

        itemProperty.setValue(propertyType, value);
        itemPropertyMapper.upsert(itemProperty);
    }

    /**
     * 다중선택 속성값 저장
     */
    private void saveMultiSelectProperty(Long itemId, Long propertyId, Object value, String username) {
        // 기존 값 삭제
        itemPropertyMapper.deleteMultiByItemIdAndPropertyId(itemId, propertyId);

        // 옵션 ID 목록 추출
        List<Long> optionIds;
        if (value instanceof List) {
            optionIds = ((List<?>) value).stream()
                    .map(v -> Long.parseLong(v.toString()))
                    .collect(Collectors.toList());
        } else if (value instanceof String) {
            String strValue = (String) value;
            optionIds = Arrays.stream(strValue.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } else {
            optionIds = List.of(Long.parseLong(value.toString()));
        }

        // 다중선택 테이블에 저장
        for (Long optionId : optionIds) {
            ItemPropertyMulti multi = ItemPropertyMulti.builder()
                    .itemId(itemId)
                    .propertyId(propertyId)
                    .optionId(optionId)
                    .createdBy(username)
                    .build();
            itemPropertyMapper.insertMulti(multi);
        }

        // VALUE_TEXT에도 콤마 구분으로 저장 (검색용)
        String textValue = optionIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ItemProperty itemProperty = ItemProperty.builder()
                .itemId(itemId)
                .propertyId(propertyId)
                .valueText(textValue)
                .createdBy(username)
                .updatedBy(username)
                .build();
        itemPropertyMapper.upsert(itemProperty);
    }
}
