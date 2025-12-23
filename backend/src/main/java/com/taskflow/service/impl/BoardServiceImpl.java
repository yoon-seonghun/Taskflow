package com.taskflow.service.impl;

import com.taskflow.domain.Board;
import com.taskflow.domain.BoardShare;
import com.taskflow.domain.Group;
import com.taskflow.domain.User;
import com.taskflow.dto.board.*;
import com.taskflow.dto.group.GroupResponse;
import com.taskflow.dto.share.ShareUpdateRequest;
import com.taskflow.dto.transfer.TransferPreviewResponse;
import com.taskflow.dto.transfer.TransferRequest;
import com.taskflow.dto.transfer.TransferResultResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.domain.PropertyDef;
import com.taskflow.mapper.BoardMapper;
import com.taskflow.mapper.BoardShareMapper;
import com.taskflow.mapper.GroupMapper;
import com.taskflow.mapper.PropertyDefMapper;
import com.taskflow.mapper.PropertyOptionMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.AuditLogService;
import com.taskflow.service.BoardService;
import com.taskflow.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 보드 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;
    private final BoardShareMapper boardShareMapper;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final PropertyDefMapper propertyDefMapper;
    private final PropertyOptionMapper propertyOptionMapper;
    private final AuditLogService auditLogService;
    @Lazy
    private final TransferService transferService;

    // =============================================
    // 보드 조회
    // =============================================

    @Override
    public BoardResponse getBoard(Long boardId) {
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 기본 정보
        BoardResponse response = BoardResponse.from(board);

        // 사용 가능한 그룹 목록 (활성 그룹만)
        List<Group> groups = groupMapper.findAll("Y");
        List<GroupResponse> groupResponses = GroupResponse.fromList(groups);

        // 담당자로 지정 가능한 사용자 목록 (소유자 + 공유 사용자)
        List<BoardResponse.SharedUserInfo> sharedUsers = new ArrayList<>();

        // 1. 소유자 추가
        userMapper.findById(board.getOwnerId()).ifPresent(owner -> {
            sharedUsers.add(BoardResponse.SharedUserInfo.builder()
                    .userId(owner.getUserId())
                    .userName(owner.getName())
                    .departmentName(owner.getDepartmentName())
                    .build());
        });

        // 2. 공유 사용자 추가
        List<BoardShare> shares = boardShareMapper.findByBoardId(boardId);
        for (BoardShare share : shares) {
            userMapper.findById(share.getUserId()).ifPresent(user -> {
                sharedUsers.add(BoardResponse.SharedUserInfo.builder()
                        .userId(user.getUserId())
                        .userName(user.getName())
                        .departmentName(user.getDepartmentName())
                        .build());
            });
        }

        // Builder로 새 객체 생성 (groups, sharedUsers 포함)
        return BoardResponse.builder()
                .boardId(response.getBoardId())
                .boardName(response.getBoardName())
                .description(response.getDescription())
                .ownerId(response.getOwnerId())
                .ownerName(response.getOwnerName())
                .defaultView(response.getDefaultView())
                .color(response.getColor())
                .sortOrder(response.getSortOrder())
                .useYn(response.getUseYn())
                .itemCount(response.getItemCount())
                .shareCount(response.getShareCount())
                .groups(groupResponses)
                .sharedUsers(sharedUsers)
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    @Override
    public List<BoardResponse> getAccessibleBoards(Long userId, String useYn) {
        List<Board> boards = boardMapper.findAccessibleByUserId(userId, useYn);
        return boards.stream()
                .map(board -> {
                    BoardResponse response = BoardResponse.from(board);
                    boolean isOwnerFlag = board.isOwner(userId);
                    String permission = isOwnerFlag ? "OWNER" : getUserPermission(board.getBoardId(), userId);
                    return BoardResponse.builder()
                            .boardId(response.getBoardId())
                            .boardName(response.getBoardName())
                            .description(response.getDescription())
                            .ownerId(response.getOwnerId())
                            .ownerName(response.getOwnerName())
                            .defaultView(response.getDefaultView())
                            .color(response.getColor())
                            .sortOrder(response.getSortOrder())
                            .useYn(response.getUseYn())
                            .itemCount(response.getItemCount())
                            .shareCount(response.getShareCount())
                            .currentUserPermission(permission)
                            .isOwner(isOwnerFlag)
                            .createdAt(response.getCreatedAt())
                            .updatedAt(response.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardResponse> getOwnedBoards(Long ownerId, String useYn) {
        List<Board> boards = boardMapper.findByOwnerId(ownerId, useYn);
        return boards.stream()
                .map(board -> {
                    BoardResponse response = BoardResponse.from(board);
                    return BoardResponse.builder()
                            .boardId(response.getBoardId())
                            .boardName(response.getBoardName())
                            .description(response.getDescription())
                            .ownerId(response.getOwnerId())
                            .ownerName(response.getOwnerName())
                            .defaultView(response.getDefaultView())
                            .color(response.getColor())
                            .sortOrder(response.getSortOrder())
                            .useYn(response.getUseYn())
                            .itemCount(response.getItemCount())
                            .shareCount(response.getShareCount())
                            .currentUserPermission("OWNER")
                            .isOwner(true)
                            .createdAt(response.getCreatedAt())
                            .updatedAt(response.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardResponse> getAllBoards(String useYn) {
        List<Board> boards = boardMapper.findAll(useYn);
        return BoardResponse.fromList(boards);
    }

    // =============================================
    // 보드 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public BoardResponse createBoard(BoardCreateRequest request, Long createdBy) {
        log.info("Creating board: name={}, owner={}", request.getBoardName(), createdBy);

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = boardMapper.getMaxSortOrder(createdBy) + 1;
        }

        // 기본 뷰 설정
        String defaultView = request.getDefaultView();
        if (defaultView == null || defaultView.isEmpty()) {
            defaultView = "TABLE";
        }

        // 보드 엔티티 생성
        Board board = Board.builder()
                .boardName(request.getBoardName())
                .description(request.getDescription())
                .ownerId(createdBy)
                .defaultView(defaultView)
                .color(request.getColor())
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        // 저장
        boardMapper.insert(board);
        log.info("Board created: id={}, name={}", board.getBoardId(), board.getBoardName());

        // 기본 속성 정의 및 옵션 생성
        createDefaultProperties(board.getBoardId(), createdBy);

        // 감사 로그 기록
        auditLogService.logBoardCreated(board.getBoardId(), createdBy, board.getBoardName());

        return getBoard(board.getBoardId());
    }

    /**
     * 신규 보드에 기본 속성 정의 및 옵션 생성
     *
     * @param boardId   보드 ID
     * @param createdBy 생성자 ID
     */
    private void createDefaultProperties(Long boardId, Long createdBy) {
        log.info("Creating default properties for board: id={}", boardId);

        // 1. 기본 속성 정의 생성 (카테고리, 상태, 우선순위, 담당자, 시작일, 마감일)
        propertyDefMapper.insertDefaultProperties(boardId, createdBy);

        // 2. 생성된 속성 조회하여 SELECT 타입 속성에 옵션 추가
        java.util.List<PropertyDef> properties = propertyDefMapper.findAllByBoardId(boardId);

        for (PropertyDef property : properties) {
            switch (property.getPropertyName()) {
                case "카테고리":
                    propertyOptionMapper.insertCategoryOptions(property.getPropertyId(), createdBy);
                    log.debug("Created category options for property: id={}", property.getPropertyId());
                    break;
                case "상태":
                    propertyOptionMapper.insertStatusOptions(property.getPropertyId(), createdBy);
                    log.debug("Created status options for property: id={}", property.getPropertyId());
                    break;
                case "우선순위":
                    propertyOptionMapper.insertPriorityOptions(property.getPropertyId(), createdBy);
                    log.debug("Created priority options for property: id={}", property.getPropertyId());
                    break;
                default:
                    // 담당자, 시작일, 마감일은 옵션이 필요 없음
                    break;
            }
        }

        log.info("Default properties created for board: id={}, propertyCount={}", boardId, properties.size());
    }

    @Override
    @Transactional
    public BoardResponse updateBoard(Long boardId, BoardUpdateRequest request, Long updatedBy) {
        log.info("Updating board: id={}", boardId);

        // 보드 존재 확인
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 소유자 확인
        if (!board.isOwner(updatedBy)) {
            throw BusinessException.accessDenied("보드 소유자만 수정할 수 있습니다.");
        }

        // 변경 전 데이터 저장
        BoardResponse beforeData = BoardResponse.from(board);

        // 수정
        board.setBoardName(request.getBoardName());
        board.setDescription(request.getDescription());
        if (request.getDefaultView() != null) {
            board.setDefaultView(request.getDefaultView());
        }
        board.setColor(request.getColor());
        if (request.getSortOrder() != null) {
            board.setSortOrder(request.getSortOrder());
        }
        if (request.getUseYn() != null) {
            board.setUseYn(request.getUseYn());
        }
        board.setUpdatedBy(updatedBy);

        boardMapper.update(board);
        log.info("Board updated: id={}", boardId);

        // 감사 로그 기록 (변경 후 데이터)
        BoardResponse afterData = getBoard(boardId);
        auditLogService.logBoardUpdated(boardId, updatedBy, beforeData, afterData);

        return afterData;
    }

    @Override
    @Transactional
    public void deleteBoard(Long boardId, Long userId) {
        log.info("Deleting board: id={}, userId={}", boardId, userId);

        // 보드 존재 확인
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 소유자 확인
        if (!board.isOwner(userId)) {
            throw BusinessException.accessDenied("보드 소유자만 삭제할 수 있습니다.");
        }

        // 아이템 존재 확인
        if (boardMapper.hasItems(boardId)) {
            throw BusinessException.dataInUse("보드에 아이템이 존재하여 삭제할 수 없습니다. 아이템을 먼저 삭제하거나 보드를 비활성화해주세요.");
        }

        // 공유 정보 삭제
        boardShareMapper.deleteByBoardId(boardId);

        // 보드 삭제
        boardMapper.delete(boardId);
        log.info("Board deleted: id={}", boardId);
    }

    // =============================================
    // 보드 공유 관리
    // =============================================

    @Override
    public List<BoardShareResponse> getBoardShares(Long boardId) {
        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        List<BoardShare> shares = boardShareMapper.findByBoardId(boardId);
        return BoardShareResponse.fromList(shares);
    }

    @Override
    @Transactional
    public BoardShareResponse addBoardShare(Long boardId, BoardShareRequest request, Long createdBy) {
        log.info("Adding board share: boardId={}, userId={}", boardId, request.getUserId());

        // 보드 존재 확인
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 소유자만 공유 가능
        if (!board.isOwner(createdBy)) {
            throw BusinessException.accessDenied("보드 소유자만 공유할 수 있습니다.");
        }

        // 소유자에게는 공유 불가
        if (board.isOwner(request.getUserId())) {
            throw BusinessException.badRequest("보드 소유자에게는 공유할 수 없습니다.");
        }

        // 사용자 존재 확인
        userMapper.findById(request.getUserId())
                .orElseThrow(() -> BusinessException.userNotFound(request.getUserId()));

        // 이미 공유되어 있는지 확인
        if (boardShareMapper.existsByBoardIdAndUserId(boardId, request.getUserId())) {
            throw BusinessException.conflict("이미 해당 사용자에게 공유되어 있습니다.");
        }

        // 권한 기본값 설정
        String permission = request.getPermission();
        if (permission == null || permission.isEmpty()) {
            permission = BoardShare.PERMISSION_MEMBER;
        }

        // 공유 추가
        BoardShare boardShare = BoardShare.builder()
                .boardId(boardId)
                .userId(request.getUserId())
                .permission(permission)
                .createdBy(createdBy)
                .build();

        boardShareMapper.insert(boardShare);
        log.info("Board share added: boardShareId={}", boardShare.getBoardShareId());

        // 감사 로그 기록
        auditLogService.logBoardShared(boardId, createdBy, request.getUserId(), permission);

        // 저장된 정보 조회하여 반환
        return BoardShareResponse.from(
                boardShareMapper.findById(boardShare.getBoardShareId()).orElse(null)
        );
    }

    @Override
    @Transactional
    public void removeBoardShare(Long boardId, Long userId, Long requestUserId) {
        log.info("Removing board share: boardId={}, userId={}", boardId, userId);

        // 보드 존재 확인
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 소유자만 공유 해제 가능
        if (!board.isOwner(requestUserId)) {
            throw BusinessException.accessDenied("보드 소유자만 공유를 해제할 수 있습니다.");
        }

        // 공유되어 있는지 확인
        if (!boardShareMapper.existsByBoardIdAndUserId(boardId, userId)) {
            throw BusinessException.notFound("해당 사용자에게 공유되어 있지 않습니다.");
        }

        // 공유 제거
        boardShareMapper.deleteByBoardIdAndUserId(boardId, userId);
        log.info("Board share removed: boardId={}, userId={}", boardId, userId);

        // 감사 로그 기록
        auditLogService.logBoardUnshared(boardId, requestUserId, userId);
    }

    // =============================================
    // 검증
    // =============================================

    @Override
    public boolean hasAccess(Long boardId, Long userId) {
        return boardMapper.hasAccess(boardId, userId);
    }

    @Override
    public boolean isOwner(Long boardId, Long userId) {
        return boardMapper.findById(boardId)
                .map(board -> board.isOwner(userId))
                .orElse(false);
    }

    // =============================================
    // 보드 관리 (신규 기능)
    // =============================================

    @Override
    public BoardResponse getBoardWithPermission(Long boardId, Long userId) {
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 접근 권한 확인
        if (!hasAccess(boardId, userId)) {
            throw BusinessException.accessDenied("보드에 접근 권한이 없습니다.");
        }

        // 기본 정보
        BoardResponse response = BoardResponse.from(board);

        // 사용 가능한 그룹 목록 (활성 그룹만)
        List<Group> groups = groupMapper.findAll("Y");
        List<GroupResponse> groupResponses = GroupResponse.fromList(groups);

        // 담당자로 지정 가능한 사용자 목록 (소유자 + 공유 사용자)
        List<BoardResponse.SharedUserInfo> sharedUsers = new ArrayList<>();

        // 1. 소유자 추가
        userMapper.findById(board.getOwnerId()).ifPresent(owner -> {
            sharedUsers.add(BoardResponse.SharedUserInfo.builder()
                    .userId(owner.getUserId())
                    .userName(owner.getName())
                    .departmentName(owner.getDepartmentName())
                    .build());
        });

        // 2. 공유 사용자 추가
        List<BoardShare> shares = boardShareMapper.findByBoardId(boardId);
        for (BoardShare share : shares) {
            userMapper.findById(share.getUserId()).ifPresent(user -> {
                sharedUsers.add(BoardResponse.SharedUserInfo.builder()
                        .userId(user.getUserId())
                        .userName(user.getName())
                        .departmentName(user.getDepartmentName())
                        .build());
            });
        }

        // 미완료 업무 수
        int pendingItemCount = boardMapper.countPendingItems(boardId);

        // 현재 사용자 권한
        String currentUserPermission = getUserPermission(boardId, userId);
        boolean isOwnerFlag = board.isOwner(userId);

        // Builder로 새 객체 생성
        return BoardResponse.builder()
                .boardId(response.getBoardId())
                .boardName(response.getBoardName())
                .description(response.getDescription())
                .ownerId(response.getOwnerId())
                .ownerName(response.getOwnerName())
                .defaultView(response.getDefaultView())
                .color(response.getColor())
                .sortOrder(response.getSortOrder())
                .useYn(response.getUseYn())
                .itemCount(response.getItemCount())
                .shareCount(response.getShareCount())
                .pendingItemCount(pendingItemCount)
                .currentUserPermission(currentUserPermission)
                .isOwner(isOwnerFlag)
                .groups(groupResponses)
                .sharedUsers(sharedUsers)
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    @Override
    public BoardListResponse getBoardList(Long userId) {
        // 소유한 보드 목록
        List<Board> ownedBoards = boardMapper.findOwnedBoards(userId);
        List<BoardResponse> ownedBoardResponses = ownedBoards.stream()
                .map(board -> {
                    BoardResponse response = BoardResponse.from(board);
                    int pendingCount = boardMapper.countPendingItems(board.getBoardId());
                    return BoardResponse.builder()
                            .boardId(response.getBoardId())
                            .boardName(response.getBoardName())
                            .description(response.getDescription())
                            .ownerId(response.getOwnerId())
                            .ownerName(response.getOwnerName())
                            .defaultView(response.getDefaultView())
                            .color(response.getColor())
                            .sortOrder(response.getSortOrder())
                            .useYn(response.getUseYn())
                            .itemCount(response.getItemCount())
                            .shareCount(response.getShareCount())
                            .pendingItemCount(pendingCount)
                            .currentUserPermission("OWNER")
                            .isOwner(true)
                            .createdAt(response.getCreatedAt())
                            .updatedAt(response.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        // 공유받은 보드 목록
        List<Map<String, Object>> sharedBoardMaps = boardMapper.findSharedBoards(userId);
        List<BoardResponse> sharedBoardResponses = sharedBoardMaps.stream()
                .map(map -> {
                    Long boardId = ((Number) map.get("BOARD_ID")).longValue();
                    String permission = (String) map.get("PERMISSION");
                    int pendingCount = boardMapper.countPendingItems(boardId);

                    return BoardResponse.builder()
                            .boardId(boardId)
                            .boardName((String) map.get("BOARD_NAME"))
                            .description((String) map.get("DESCRIPTION"))
                            .ownerId(((Number) map.get("OWNER_ID")).longValue())
                            .ownerName((String) map.get("OWNER_NAME"))
                            .defaultView((String) map.get("DEFAULT_VIEW"))
                            .color((String) map.get("COLOR"))
                            .sortOrder(map.get("SORT_ORDER") != null ? ((Number) map.get("SORT_ORDER")).intValue() : null)
                            .useYn((String) map.get("USE_YN"))
                            .itemCount(map.get("ITEM_COUNT") != null ? ((Number) map.get("ITEM_COUNT")).intValue() : 0)
                            .shareCount(map.get("SHARE_COUNT") != null ? ((Number) map.get("SHARE_COUNT")).intValue() : 0)
                            .pendingItemCount(pendingCount)
                            .currentUserPermission(permission)
                            .isOwner(false)
                            .build();
                })
                .collect(Collectors.toList());

        return BoardListResponse.builder()
                .ownedBoards(ownedBoardResponses)
                .sharedBoards(sharedBoardResponses)
                .totalOwnedCount(ownedBoardResponses.size())
                .totalSharedCount(sharedBoardResponses.size())
                .build();
    }

    @Override
    @Transactional
    public void updateBoardOrder(Long boardId, Integer sortOrder, Long userId) {
        log.info("Updating board order: boardId={}, sortOrder={}", boardId, sortOrder);

        // 보드 존재 확인
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 소유자 확인
        if (!board.isOwner(userId)) {
            throw BusinessException.accessDenied("보드 소유자만 순서를 변경할 수 있습니다.");
        }

        // 순서 변경
        boardMapper.updateSortOrder(boardId, sortOrder, userId);
        log.info("Board order updated: boardId={}, sortOrder={}", boardId, sortOrder);
    }

    @Override
    @Transactional
    public TransferResultResponse deleteBoardWithTransfer(Long boardId, BoardDeleteRequest request, Long userId) {
        log.info("Deleting board with transfer: boardId={}, userId={}", boardId, userId);

        // 보드 존재 확인
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 소유자 확인
        if (!board.isOwner(userId)) {
            throw BusinessException.accessDenied("보드 소유자만 삭제할 수 있습니다.");
        }

        TransferResultResponse transferResult = null;

        // 미완료 업무가 있고, 이관 대상자가 지정된 경우 이관 실행
        int pendingCount = boardMapper.countPendingItems(boardId);
        if (pendingCount > 0 && request.getTargetUserId() != null) {
            // 이관 요청 생성
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setTargetUserId(request.getTargetUserId());

            // 미완료 업무 ID 목록 조회
            List<Long> pendingItemIds = boardMapper.findPendingItems(boardId).stream()
                    .map(m -> ((Number) m.get("ITEM_ID")).longValue())
                    .collect(Collectors.toList());
            transferRequest.setItemIds(pendingItemIds);

            // 이관 실행
            transferResult = transferService.executeTransfer(boardId, transferRequest, userId);
        }

        // 강제 삭제 또는 이관 완료 후 삭제
        if (request.isForceDelete() || pendingCount == 0 || transferResult != null) {
            // 공유 정보 삭제
            boardShareMapper.deleteByBoardId(boardId);

            // 보드 비활성화 (소프트 삭제)
            board.setUseYn("N");
            board.setUpdatedBy(userId);
            boardMapper.update(board);

            // 감사 로그 기록
            auditLogService.logBoardDeleted(boardId, userId, board.getBoardName());

            log.info("Board deleted: boardId={}", boardId);
        }

        return transferResult;
    }

    @Override
    public TransferPreviewResponse getTransferPreview(Long boardId) {
        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        return transferService.getTransferPreview(boardId);
    }

    @Override
    @Transactional
    public void updateBoardSharePermission(Long boardId, Long userId, ShareUpdateRequest request, Long requestUserId) {
        log.info("Updating board share permission: boardId={}, userId={}, permission={}",
                boardId, userId, request.getPermission());

        // 보드 존재 확인
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 소유자만 권한 변경 가능
        if (!board.isOwner(requestUserId)) {
            throw BusinessException.accessDenied("보드 소유자만 권한을 변경할 수 있습니다.");
        }

        // 공유되어 있는지 확인
        if (!boardShareMapper.existsByBoardIdAndUserId(boardId, userId)) {
            throw BusinessException.notFound("해당 사용자에게 공유되어 있지 않습니다.");
        }

        // 기존 권한 조회
        String oldPermission = boardMapper.getUserPermission(boardId, userId);

        // 권한 변경
        boardShareMapper.updatePermissionByBoardAndUser(boardId, userId, request.getPermission(), requestUserId);
        log.info("Board share permission updated: boardId={}, userId={}, permission={}",
                boardId, userId, request.getPermission());

        // 감사 로그 기록
        auditLogService.log(
                "BOARD_SHARE", boardId, "UPDATE",
                requestUserId,
                String.format("보드 공유 권한 변경: %s → %s", oldPermission, request.getPermission()),
                null, null, userId
        );
    }

    @Override
    public String getUserPermission(Long boardId, Long userId) {
        // 보드 존재 확인
        Board board = boardMapper.findById(boardId).orElse(null);
        if (board == null) {
            return null;
        }

        // 소유자인 경우
        if (board.isOwner(userId)) {
            return "OWNER";
        }

        // 공유 권한 조회
        return boardMapper.getUserPermission(boardId, userId);
    }

    @Override
    public boolean canEdit(Long boardId, Long userId) {
        String permission = getUserPermission(boardId, userId);
        if (permission == null) {
            return false;
        }
        return "OWNER".equals(permission) ||
               BoardShare.PERMISSION_EDIT.equals(permission) ||
               BoardShare.PERMISSION_FULL.equals(permission);
    }

    @Override
    public boolean canDelete(Long boardId, Long userId) {
        String permission = getUserPermission(boardId, userId);
        if (permission == null) {
            return false;
        }
        return "OWNER".equals(permission) || BoardShare.PERMISSION_FULL.equals(permission);
    }

    // =============================================
    // 보드 소유권 이전
    // =============================================

    // 보드 이관 시 자동 변경되는 보드명
    private static final String TRANSFERRED_BOARD_NAME = "보드이관";

    @Override
    @Transactional
    public BoardResponse transferBoardOwnership(Long boardId, BoardTransferRequest request, Long currentUserId) {
        log.info("Transferring board ownership: boardId={}, targetUserId={}", boardId, request.getTargetUserId());

        // 보드 존재 확인
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 소유자 확인
        if (!board.isOwner(currentUserId)) {
            throw BusinessException.accessDenied("보드 소유자만 이관할 수 있습니다.");
        }

        // 자기 자신에게 이관 불가
        if (currentUserId.equals(request.getTargetUserId())) {
            throw BusinessException.badRequest("자기 자신에게 이관할 수 없습니다.");
        }

        // 대상 사용자 존재 확인
        User targetUser = userMapper.findById(request.getTargetUserId())
                .orElseThrow(() -> BusinessException.userNotFound(request.getTargetUserId()));

        String originalBoardName = board.getBoardName();
        Long originalOwnerId = board.getOwnerId();

        // 보드 소유권 이전 (보드명 -> "보드이관"으로 변경)
        int updated = boardMapper.transferOwnership(
                boardId,
                request.getTargetUserId(),
                TRANSFERRED_BOARD_NAME,
                currentUserId
        );

        if (updated == 0) {
            throw BusinessException.badRequest("보드 이관에 실패했습니다.");
        }

        // 기존 소유자를 공유 사용자에서 제거 (이미 공유되어 있었다면)
        if (boardShareMapper.existsByBoardIdAndUserId(boardId, request.getTargetUserId())) {
            boardShareMapper.deleteByBoardIdAndUserId(boardId, request.getTargetUserId());
        }

        // 감사 로그 기록
        String description = String.format("보드 이관: %s → %s (보드명: %s → %s)",
                originalOwnerId, request.getTargetUserId(),
                originalBoardName, TRANSFERRED_BOARD_NAME);
        auditLogService.log(
                "BOARD", boardId, "TRANSFER",
                currentUserId, description,
                null, null, request.getTargetUserId()
        );

        log.info("Board ownership transferred: boardId={}, from userId={} to userId={}",
                boardId, originalOwnerId, request.getTargetUserId());

        return getBoard(boardId);
    }
}
