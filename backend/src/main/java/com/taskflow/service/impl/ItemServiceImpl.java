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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // v2.0: 카테고리 기반 속성 상속용 Mapper
    private final BoardCategoryMapper boardCategoryMapper;
    private final CategoryPropertyMapper categoryPropertyMapper;
    private final BoardPropertyMapper boardPropertyMapper;

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

        // v2.0: 카테고리 결정 (요청에 없으면 보드의 기본 카테고리 사용)
        Long categoryId = request.getCategoryId();
        if (categoryId == null) {
            Optional<BoardCategory> defaultCategory = boardCategoryMapper.findDefaultByBoardId(boardId);
            if (defaultCategory.isPresent()) {
                categoryId = defaultCategory.get().getCategoryId();
                log.debug("Using default category: boardId={}, categoryId={}", boardId, categoryId);
            }
        }

        // 기본값 설정
        String status = request.getStatus() != null ? request.getStatus() : Item.STATUS_NOT_STARTED;
        String priority = request.getPriority() != null ? request.getPriority() : Item.PRIORITY_NORMAL;

        // 정렬 순서 설정 (현재 최대값 + 1)
        Integer maxSortOrder = itemMapper.getMaxSortOrder(boardId);
        Integer sortOrder = (maxSortOrder != null ? maxSortOrder : -1) + 1;

        // 아이템 엔티티 생성
        Item item = Item.builder()
                .boardId(boardId)
                .groupId(request.getGroupId())
                .categoryId(categoryId)
                .title(request.getTitle())
                .content(request.getContent())
                .description(request.getDescription())
                .status(status)
                .priority(priority)
                .assigneeUsername(request.getAssigneeUsername())
                .requestDate(request.getRequestDate())
                .dueDate(request.getDueDate())
                .sortOrder(sortOrder)
                .createdBy(createdBy)
                .build();

        // 아이템 저장
        itemMapper.insert(item);
        log.info("Item created: id={}", item.getItemId());

        // v2.0: 카테고리 속성 상속 (기본값 적용)
        if (categoryId != null) {
            inheritCategoryProperties(item.getItemId(), boardId, categoryId, request.getProperties(), createdBy);
        }

        // 동적 속성값 저장 (사용자가 명시적으로 지정한 속성값 - 상속값을 덮어씀)
        if (request.getProperties() != null && !request.getProperties().isEmpty()) {
            saveItemProperties(item.getItemId(), boardId, request.getProperties(),
                    request.getPropertySortOrders(), createdBy);
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

        // v2.0: 카테고리 변경 감지 (기존 categoryId 저장)
        Long oldCategoryId = item.getCategoryId();
        Long newCategoryId = null;
        boolean categoryChanged = false;

        if (clearFields.contains("categoryId")) {
            newCategoryId = null;
            categoryChanged = (oldCategoryId != null);
        } else if (request.getCategoryId() != null) {
            newCategoryId = request.getCategoryId();
            categoryChanged = !request.getCategoryId().equals(oldCategoryId);
        }

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

        // v2.0: 카테고리 변경 시 속성값 처리
        if (categoryChanged) {
            handleCategoryChange(itemId, item.getBoardId(), oldCategoryId, newCategoryId,
                                 request.getProperties(), updatedBy);
        }

        // 동적 속성값 저장 (카테고리 변경 처리 후 사용자 지정값 적용)
        if (request.getProperties() != null && !request.getProperties().isEmpty()) {
            saveItemProperties(itemId, item.getBoardId(), request.getProperties(),
                    request.getPropertySortOrders(), updatedBy);
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

    @Override
    @Transactional
    public void reorderItem(Long boardId, com.taskflow.dto.item.ItemReorderRequest request, String updatedBy) {
        log.info("Reordering item: boardId={}, itemId={}, newOrder={}",
                boardId, request.getItemId(), request.getNewOrder());

        // 아이템 존재 및 보드 소속 확인
        Item item = itemMapper.findById(request.getItemId())
                .orElseThrow(() -> BusinessException.itemNotFound(request.getItemId()));

        if (!boardId.equals(item.getBoardId())) {
            throw BusinessException.badRequest("아이템이 해당 보드에 속하지 않습니다.");
        }

        Integer currentOrder = item.getSortOrder();
        Integer newOrder = request.getNewOrder();

        // 동일 순서면 무시
        if (currentOrder != null && currentOrder.equals(newOrder)) {
            log.debug("Same order, skipping: itemId={}, order={}", request.getItemId(), newOrder);
            return;
        }

        // 순서 변경 처리
        if (currentOrder == null || currentOrder > newOrder) {
            // 위로 이동: 범위 내 아이템 순서 증가
            itemMapper.incrementSortOrderInRange(boardId, newOrder, currentOrder != null ? currentOrder : Integer.MAX_VALUE, request.getItemId());
        } else {
            // 아래로 이동: 범위 내 아이템 순서 감소
            itemMapper.decrementSortOrderInRange(boardId, currentOrder, newOrder, request.getItemId());
        }

        // 해당 아이템 순서 업데이트
        itemMapper.updateSortOrder(request.getItemId(), newOrder, updatedBy);

        log.info("Item reordered: itemId={}, from={} to={}", request.getItemId(), currentOrder, newOrder);

        // SSE 이벤트 발행 (아이템 갱신)
        ItemResponse response = getItem(request.getItemId());
        sseEventPublisher.publishItemUpdated(boardId, response, updatedBy);
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
    // 공유받은 업무 조회
    // =============================================

    @Override
    public ItemPageResponse getSharedItems(String username, CrossBoardSearchRequest request) {
        log.debug("Get shared items: username={}, request={}", username, request);

        // 공유받은 아이템 목록 조회
        List<Item> items = itemMapper.findSharedItems(username, request);

        // 동적 속성값 로드
        loadItemPropertiesBatch(items);

        // 총 개수 조회
        long totalElements = itemMapper.countSharedItems(username, request);

        // 응답 변환
        List<ItemResponse> content = ItemResponse.fromList(items);

        return ItemPageResponse.of(content, request.getPage(), request.getSize(), totalElements);
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
     *
     * v2.0: 글로벌/매니저 속성 지원
     * - GLOBAL 속성: boardId가 null, 모든 보드에서 사용 가능
     * - MANAGER 속성: boardId가 null, 소유자/부서 기준으로 사용 가능
     * - BOARD 속성: boardId가 지정됨, 해당 보드에서만 사용 가능
     *
     * @param itemId             아이템 ID
     * @param boardId            보드 ID
     * @param propertyValues     속성값 맵 (propertyId -> value)
     * @param propertySortOrders 속성 정렬 순서 맵 (propertyId -> sortOrder), null 가능
     * @param username           사용자명
     */
    private void saveItemProperties(Long itemId, Long boardId, Map<Long, Object> propertyValues,
                                    Map<Long, Integer> propertySortOrders, String username) {
        for (Map.Entry<Long, Object> entry : propertyValues.entrySet()) {
            Long propertyId = entry.getKey();
            Object value = entry.getValue();

            // 속성 정의 조회
            PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                    .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

            // v2.0: 속성 사용 권한 확인
            // - GLOBAL, MANAGER, USER 타입: boardId 무관하게 사용 가능
            // - 레거시(boardId가 있는 속성): 보드 일치 확인
            String ownerType = propertyDef.getOwnerType();
            if (ownerType == null && propertyDef.getBoardId() != null) {
                // ownerType이 없고 boardId가 있는 레거시 속성: 보드 일치 확인
                if (!boardId.equals(propertyDef.getBoardId())) {
                    throw BusinessException.badRequest("속성이 해당 보드에 속하지 않습니다: " + propertyId);
                }
            }
            // GLOBAL, MANAGER, USER 속성은 boardId와 무관하게 사용 가능 (카테고리를 통해 보드에 연결됨)

            // sortOrder 가져오기
            Integer sortOrder = (propertySortOrders != null) ? propertySortOrders.get(propertyId) : null;

            // 값이 null이면 삭제 (빈 문자열 ""은 "선택됨" 상태로 유지 - 프론트엔드 의도)
            if (value == null) {
                itemPropertyMapper.deleteByItemIdAndPropertyId(itemId, propertyId);
                if (PropertyDef.TYPE_MULTI_SELECT.equals(propertyDef.getPropertyType())) {
                    itemPropertyMapper.deleteMultiByItemIdAndPropertyId(itemId, propertyId);
                }
                continue;
            }

            // 빈 문자열인 경우 - 속성 타입에 따라 처리 (ItemProperty.setValue에서 null로 변환됨)
            // TEXT 타입은 빈 문자열 그대로 저장, 나머지는 null로 저장되어 "선택됨" 상태 유지

            // 다중선택의 경우 특별 처리
            if (PropertyDef.TYPE_MULTI_SELECT.equals(propertyDef.getPropertyType())) {
                saveMultiSelectProperty(itemId, propertyId, value, sortOrder, username);
            } else {
                saveSingleProperty(itemId, propertyId, propertyDef.getPropertyType(), value, sortOrder, username);
            }
        }
    }

    /**
     * 단일 값 속성 저장
     *
     * @param itemId       아이템 ID
     * @param propertyId   속성 ID
     * @param propertyType 속성 타입
     * @param value        값
     * @param sortOrder    정렬 순서 (null이면 기본값 0)
     * @param username     사용자명
     */
    private void saveSingleProperty(Long itemId, Long propertyId, String propertyType, Object value,
                                    Integer sortOrder, String username) {
        ItemProperty itemProperty = ItemProperty.builder()
                .itemId(itemId)
                .propertyId(propertyId)
                .sortOrder(sortOrder)
                .createdBy(username)
                .updatedBy(username)
                .build();

        itemProperty.setValue(propertyType, value);
        itemPropertyMapper.upsert(itemProperty);
    }

    /**
     * 다중선택 속성값 저장
     *
     * @param itemId     아이템 ID
     * @param propertyId 속성 ID
     * @param value      값 (List 또는 콤마 구분 문자열)
     * @param sortOrder  정렬 순서 (null이면 기본값 0)
     * @param username   사용자명
     */
    private void saveMultiSelectProperty(Long itemId, Long propertyId, Object value,
                                         Integer sortOrder, String username) {
        // 기존 값 삭제
        itemPropertyMapper.deleteMultiByItemIdAndPropertyId(itemId, propertyId);

        // 속성 정의 조회하여 데이터 소스 타입 확인
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId).orElse(null);
        boolean isExternalQuery = propertyDef != null && "EXTERNAL".equals(propertyDef.getDataSourceType());

        // 값 목록 추출 (문자열 목록)
        List<String> stringValues;
        if (value instanceof List) {
            stringValues = ((List<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else if (value instanceof String) {
            String strValue = (String) value;
            stringValues = Arrays.stream(strValue.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            stringValues = List.of(value.toString());
        }

        // 외부 쿼리인 경우: 문자열 값 그대로 저장 (TB_ITEM_PROPERTY_MULTI 사용 안함)
        // 내부 옵션인 경우: 옵션 ID로 파싱하여 TB_ITEM_PROPERTY_MULTI에 저장
        if (!isExternalQuery) {
            // 내부 옵션: optionId로 파싱
            for (String strVal : stringValues) {
                try {
                    Long optionId = Long.parseLong(strVal);
                    ItemPropertyMulti multi = ItemPropertyMulti.builder()
                            .itemId(itemId)
                            .propertyId(propertyId)
                            .optionId(optionId)
                            .createdBy(username)
                            .build();
                    itemPropertyMapper.insertMulti(multi);
                } catch (NumberFormatException e) {
                    log.warn("Invalid optionId for internal MULTI_SELECT: {}", strVal);
                }
            }
        }
        // 외부 쿼리인 경우 TB_ITEM_PROPERTY_MULTI에는 저장하지 않음 (문자열 값이므로)

        // VALUE_TEXT에 콤마 구분으로 저장 (검색용 & 외부 쿼리 값 저장용)
        String textValue = String.join(",", stringValues);

        ItemProperty itemProperty = ItemProperty.builder()
                .itemId(itemId)
                .propertyId(propertyId)
                .valueText(textValue)
                .sortOrder(sortOrder)
                .createdBy(username)
                .updatedBy(username)
                .build();
        itemPropertyMapper.upsert(itemProperty);
    }

    // =============================================
    // v2.0: 카테고리 기반 속성 상속
    // =============================================

    /**
     * 카테고리 속성 상속
     *
     * 카테고리에 정의된 속성들의 기본값을 업무에 자동 적용합니다.
     * - BoardProperty가 있으면 BoardProperty의 설정(기본값, 필수여부) 우선 적용
     * - BoardProperty가 없으면 CategoryProperty의 설정 적용
     * - 사용자가 명시적으로 지정한 속성값은 나중에 덮어씀 (saveItemProperties에서 처리)
     *
     * @param itemId          업무 ID
     * @param boardId         보드 ID
     * @param categoryId      카테고리 ID
     * @param userProperties  사용자가 명시적으로 지정한 속성값 (null이면 모든 기본값 적용)
     * @param createdBy       생성자
     */
    private void inheritCategoryProperties(Long itemId, Long boardId, Long categoryId,
                                           Map<Long, Object> userProperties, String createdBy) {
        log.debug("Inheriting category properties: itemId={}, boardId={}, categoryId={}",
                itemId, boardId, categoryId);

        // 1. 카테고리에 정의된 속성 목록 조회
        List<CategoryProperty> categoryProperties = categoryPropertyMapper.findByCategoryId(categoryId);
        if (categoryProperties.isEmpty()) {
            log.debug("No properties defined for category: categoryId={}", categoryId);
            return;
        }

        // 2. 보드별 속성 설정 조회 (BoardProperty - 기본값, 필수여부 재정의)
        List<BoardProperty> boardProperties = boardPropertyMapper.findByBoardId(boardId);
        Map<Long, BoardProperty> boardPropertyMap = boardProperties.stream()
                .collect(Collectors.toMap(BoardProperty::getPropertyId, bp -> bp, (a, b) -> a));

        // 3. 사용자가 명시적으로 지정한 속성 ID 집합
        java.util.Set<Long> userSpecifiedPropertyIds = (userProperties != null)
                ? userProperties.keySet()
                : java.util.Collections.emptySet();

        int inheritedCount = 0;

        // 4. 각 카테고리 속성에 대해 기본값 적용
        for (CategoryProperty cp : categoryProperties) {
            Long propertyId = cp.getPropertyId();

            // 사용자가 명시적으로 값을 지정한 경우 상속하지 않음 (나중에 saveItemProperties에서 적용됨)
            if (userSpecifiedPropertyIds.contains(propertyId)) {
                log.debug("Skipping property (user specified): propertyId={}", propertyId);
                continue;
            }

            // BoardProperty가 있으면 해당 설정 사용, 없으면 CategoryProperty 설정 사용
            String defaultValue;
            BoardProperty bp = boardPropertyMap.get(propertyId);
            if (bp != null && bp.getDefaultValue() != null && !bp.getDefaultValue().isEmpty()) {
                defaultValue = bp.getDefaultValue();
                log.debug("Using BoardProperty defaultValue: propertyId={}, value={}", propertyId, defaultValue);
            } else {
                defaultValue = cp.getDefaultValue();
                log.debug("Using CategoryProperty defaultValue: propertyId={}, value={}", propertyId, defaultValue);
            }

            // 기본값이 없으면 건너뜀
            if (defaultValue == null || defaultValue.isEmpty()) {
                continue;
            }

            // 속성 정의 조회
            Optional<PropertyDef> propertyDefOpt = propertyDefMapper.findById(propertyId);
            if (propertyDefOpt.isEmpty()) {
                log.warn("Property definition not found: propertyId={}", propertyId);
                continue;
            }

            PropertyDef propertyDef = propertyDefOpt.get();

            // 속성값 저장 (상속 속성은 sortOrder=null, 기본값 0 사용)
            try {
                if (PropertyDef.TYPE_MULTI_SELECT.equals(propertyDef.getPropertyType())) {
                    saveMultiSelectProperty(itemId, propertyId, defaultValue, null, createdBy);
                } else {
                    saveSingleProperty(itemId, propertyId, propertyDef.getPropertyType(), defaultValue, null, createdBy);
                }
                inheritedCount++;
            } catch (Exception e) {
                log.warn("Failed to inherit property default value: propertyId={}, value={}, error={}",
                        propertyId, defaultValue, e.getMessage());
            }
        }

        log.info("Category properties inherited: itemId={}, categoryId={}, inheritedCount={}",
                itemId, categoryId, inheritedCount);
    }

    /**
     * 카테고리 변경 시 속성값 처리
     *
     * v2.0: 업무 카테고리 변경 시 기존 속성값을 정리하고 새 카테고리 속성을 상속합니다.
     * - 기존 카테고리 전용 속성값은 폐기 (새 카테고리에 없는 속성)
     * - 동일 속성명을 가진 속성은 값 유지 (사용자 편의)
     * - 새 카테고리 속성의 기본값 상속
     *
     * @param itemId          업무 ID
     * @param boardId         보드 ID
     * @param oldCategoryId   기존 카테고리 ID (null 가능)
     * @param newCategoryId   새 카테고리 ID (null 가능)
     * @param userProperties  사용자가 명시적으로 지정한 속성값 (null 가능)
     * @param updatedBy       수정자
     */
    private void handleCategoryChange(Long itemId, Long boardId, Long oldCategoryId, Long newCategoryId,
                                       Map<Long, Object> userProperties, String updatedBy) {
        log.info("Handling category change: itemId={}, oldCategoryId={}, newCategoryId={}",
                itemId, oldCategoryId, newCategoryId);

        // 1. 기존 카테고리 속성 목록 조회
        List<CategoryProperty> oldCategoryProperties = (oldCategoryId != null)
                ? categoryPropertyMapper.findByCategoryId(oldCategoryId)
                : new ArrayList<>();

        // 2. 새 카테고리 속성 목록 조회
        List<CategoryProperty> newCategoryProperties = (newCategoryId != null)
                ? categoryPropertyMapper.findByCategoryId(newCategoryId)
                : new ArrayList<>();

        // 3. 새 카테고리 속성명 맵 생성 (속성명 → 속성ID)
        Map<String, Long> newPropertyNameToId = new HashMap<>();
        for (CategoryProperty cp : newCategoryProperties) {
            if (cp.getPropertyName() != null) {
                newPropertyNameToId.put(cp.getPropertyName(), cp.getPropertyId());
            }
        }

        // 4. 새 카테고리 속성 ID 집합
        java.util.Set<Long> newPropertyIds = newCategoryProperties.stream()
                .map(CategoryProperty::getPropertyId)
                .collect(Collectors.toSet());

        // 5. 기존 속성값 중 새 카테고리에 없는 것은 삭제 (동일 속성명이면 유지)
        int deletedCount = 0;
        int preservedCount = 0;

        for (CategoryProperty oldCp : oldCategoryProperties) {
            Long oldPropertyId = oldCp.getPropertyId();
            String oldPropertyName = oldCp.getPropertyName();

            // 새 카테고리에 동일 속성 ID가 있으면 유지
            if (newPropertyIds.contains(oldPropertyId)) {
                preservedCount++;
                continue;
            }

            // 새 카테고리에 동일 속성명이 있으면 값 이전 시도
            Long matchingNewPropertyId = newPropertyNameToId.get(oldPropertyName);
            if (matchingNewPropertyId != null) {
                // 기존 속성값 조회
                Optional<ItemProperty> existingValue = itemPropertyMapper.findByItemIdAndPropertyId(itemId, oldPropertyId);
                if (existingValue.isPresent()) {
                    ItemProperty oldValue = existingValue.get();

                    // 새 속성 정의 조회
                    Optional<PropertyDef> newPropDefOpt = propertyDefMapper.findById(matchingNewPropertyId);
                    if (newPropDefOpt.isPresent()) {
                        PropertyDef newPropDef = newPropDefOpt.get();

                        // 기존 값으로 새 속성에 저장 (타입 호환 시, sortOrder=null)
                        try {
                            String valueToTransfer = getTransferableValue(oldValue);
                            if (valueToTransfer != null && !valueToTransfer.isEmpty()) {
                                saveSingleProperty(itemId, matchingNewPropertyId,
                                        newPropDef.getPropertyType(), valueToTransfer, null, updatedBy);
                                log.debug("Property value transferred by name: '{}' ({} -> {})",
                                        oldPropertyName, oldPropertyId, matchingNewPropertyId);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to transfer property value: {} -> {}, error={}",
                                    oldPropertyId, matchingNewPropertyId, e.getMessage());
                        }
                    }
                }
                preservedCount++;
            }

            // 기존 속성값 삭제 (새 카테고리에 없거나 이전 완료된 경우)
            itemPropertyMapper.deleteByItemIdAndPropertyId(itemId, oldPropertyId);

            // 다중선택인 경우 멀티 테이블도 삭제
            Optional<PropertyDef> oldPropDefOpt = propertyDefMapper.findById(oldPropertyId);
            if (oldPropDefOpt.isPresent()
                    && PropertyDef.TYPE_MULTI_SELECT.equals(oldPropDefOpt.get().getPropertyType())) {
                itemPropertyMapper.deleteMultiByItemIdAndPropertyId(itemId, oldPropertyId);
            }

            deletedCount++;
        }

        log.info("Old category properties processed: deleted={}, preserved={}",
                deletedCount, preservedCount);

        // 6. 새 카테고리 속성 상속 (기본값 적용)
        if (newCategoryId != null) {
            inheritCategoryProperties(itemId, boardId, newCategoryId, userProperties, updatedBy);
        }
    }

    /**
     * ItemProperty에서 이전 가능한 값 추출
     */
    private String getTransferableValue(ItemProperty itemProperty) {
        if (itemProperty == null) {
            return null;
        }

        // 값 우선순위: TEXT > NUMBER > DATE > SELECT
        if (itemProperty.getValueText() != null && !itemProperty.getValueText().isEmpty()) {
            return itemProperty.getValueText();
        }
        if (itemProperty.getValueNumber() != null) {
            return itemProperty.getValueNumber().toString();
        }
        if (itemProperty.getValueDate() != null) {
            return itemProperty.getValueDate().toString();
        }
        if (itemProperty.getValueOptionId() != null) {
            return itemProperty.getValueOptionId().toString();
        }

        return null;
    }

    // =============================================
    // 아이템 속성 순서 변경
    // =============================================

    @Override
    @Transactional
    public void updatePropertySortOrders(Long itemId, ItemPropertySortRequest request, String updatedBy) {
        log.info("Updating item property sort orders: itemId={}, ordersCount={}",
                itemId, request.getOrders() != null ? request.getOrders().size() : 0);

        // 아이템 존재 확인
        itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        if (request.getOrders() == null || request.getOrders().isEmpty()) {
            log.warn("No sort orders provided for itemId={}", itemId);
            return;
        }

        // Mapper의 내부 클래스로 변환
        List<ItemPropertyMapper.ItemPropertySortOrder> orders = request.getOrders().stream()
                .map(o -> new ItemPropertyMapper.ItemPropertySortOrder(o.getItemPropertyId(), o.getSortOrder()))
                .collect(Collectors.toList());

        // 일괄 업데이트
        int updatedCount = itemPropertyMapper.updateSortOrders(orders, updatedBy);
        log.info("Item property sort orders updated: itemId={}, updatedCount={}", itemId, updatedCount);
    }
}
