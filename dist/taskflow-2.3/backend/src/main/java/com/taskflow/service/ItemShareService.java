package com.taskflow.service;

import com.taskflow.domain.Board;
import com.taskflow.domain.Item;
import com.taskflow.domain.ItemShare;
import com.taskflow.dto.item.ItemResponse;
import com.taskflow.dto.item.ItemShareResponse;
import com.taskflow.dto.item.ItemTransferRequest;
import com.taskflow.dto.share.ShareRequest;
import com.taskflow.dto.share.ShareResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.BoardMapper;
import com.taskflow.mapper.ItemMapper;
import com.taskflow.mapper.ItemShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 업무 공유 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemShareService {

    private final ItemShareMapper itemShareMapper;
    private final ItemMapper itemMapper;
    private final BoardMapper boardMapper;
    private final AuditLogService auditLogService;

    /**
     * 업무 공유 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ShareResponse> getShares(Long itemId) {
        return itemShareMapper.selectByItemId(itemId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 공유 추가
     */
    @Transactional
    public void addShare(Long itemId, ShareRequest request, String currentUsername) {
        // 업무 존재 확인
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> new BusinessException("업무를 찾을 수 없습니다."));

        // 중복 확인
        if (itemShareMapper.existsByItemIdAndUsername(itemId, request.getUsername())) {
            throw new BusinessException("이미 공유된 사용자입니다.");
        }

        // v2.2.1: 소유자에게 공유 불가 (기존: 생성자)
        if (request.getUsername().equals(item.getOwnerUsername())) {
            throw new BusinessException("업무 소유자에게는 공유할 수 없습니다.");
        }

        ItemShare itemShare = ItemShare.builder()
                .itemId(itemId)
                .username(request.getUsername())
                .shareType("SHARE")  // 공유 유형 명시 (SHARE/ASSIGN)
                .permission(request.getPermission())
                .createdBy(currentUsername)
                .build();

        itemShareMapper.insert(itemShare);

        // 감사 로그 기록
        auditLogService.logItemShared(itemId, currentUsername, request.getUsername(), request.getPermission());

        log.info("Item {} shared with user {} (permission: {})", itemId, request.getUsername(), request.getPermission());
    }

    /**
     * 권한 변경
     */
    @Transactional
    public void updatePermission(Long itemId, String username, String permission, String currentUsername) {
        // 기존 공유 확인
        ItemShare existing = itemShareMapper.selectByItemIdAndUsername(itemId, username);
        if (existing == null) {
            throw new BusinessException("공유 정보를 찾을 수 없습니다.");
        }

        String oldPermission = existing.getPermission();
        itemShareMapper.updatePermission(itemId, username, permission, currentUsername);

        // 감사 로그 기록
        auditLogService.log(
                "ITEM_SHARE", itemId, "UPDATE",
                currentUsername, String.format("권한 변경: %s → %s", oldPermission, permission),
                null, null, username
        );

        log.info("Item {} share permission changed for user {} ({} -> {})",
                itemId, username, oldPermission, permission);
    }

    /**
     * 공유 제거
     */
    @Transactional
    public void removeShare(Long itemId, String username, String currentUsername) {
        // 기존 공유 확인
        if (!itemShareMapper.existsByItemIdAndUsername(itemId, username)) {
            throw new BusinessException("공유 정보를 찾을 수 없습니다.");
        }

        itemShareMapper.delete(itemId, username);

        // 감사 로그 기록
        auditLogService.logItemUnshared(itemId, currentUsername, username);

        log.info("Item {} share removed for user {}", itemId, username);
    }

    /**
     * 사용자의 업무 접근 권한 확인
     */
    @Transactional(readOnly = true)
    public String getPermission(Long itemId, String username) {
        return itemShareMapper.getPermission(itemId, username);
    }

    /**
     * 사용자가 업무에 접근 가능한지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(Long itemId, String username) {
        // 업무 조회
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // 생성자인 경우
        if (username.equals(item.getCreatedBy())) {
            return true;
        }

        // 공유받은 경우
        return itemShareMapper.existsByItemIdAndUsername(itemId, username);
    }

    /**
     * 수정 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean canEdit(Long itemId, String username) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // 생성자인 경우
        if (username.equals(item.getCreatedBy())) {
            return true;
        }

        // 공유 권한 확인
        String permission = getPermission(itemId, username);
        return ItemShare.PERMISSION_EDIT.equals(permission) || ItemShare.PERMISSION_FULL.equals(permission);
    }

    /**
     * 삭제 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean canDelete(Long itemId, String username) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // 생성자인 경우
        if (username.equals(item.getCreatedBy())) {
            return true;
        }

        // 공유 권한 확인
        String permission = getPermission(itemId, username);
        return ItemShare.PERMISSION_FULL.equals(permission);
    }

    private ShareResponse toResponse(ItemShare share) {
        return ShareResponse.builder()
                .username(share.getUsername())
                .userName(share.getUserName())
                .departmentName(share.getDepartmentName())
                .permission(share.getPermission())
                .createdAt(share.getCreatedAt())
                .build();
    }

    // =============================================
    // 개별 업무 이관 기능
    // =============================================

    // 업무 이관 시 자동 생성되는 보드명
    private static final String TRANSFER_BOARD_NAME = "업무이관";

    /**
     * 개별 업무 이관
     * - 다른 보드로 이관: targetBoardId 사용
     * - 다른 사용자에게 이관: targetUsername 사용 (사용자의 "업무이관" 보드로 자동 이관)
     */
    @Transactional
    public ItemResponse transferItem(Long itemId, ItemTransferRequest request, String currentUsername) {
        // 업무 조회
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> new BusinessException("업무를 찾을 수 없습니다."));

        // v2.2: 하위 업무는 단독 이관 불가
        if (item.getItemDepth() != null && item.getItemDepth() > 0) {
            throw new BusinessException("하위 업무는 단독으로 이관할 수 없습니다. 기본 업무를 이관해주세요.");
        }

        // 이관 권한 확인
        if (!canTransfer(itemId, currentUsername)) {
            throw new BusinessException("업무를 이관할 권한이 없습니다.");
        }

        Long targetBoardId = request.getTargetBoardId();
        String targetBoardName = null;
        String newOwnerUsername = null;  // v2.2.1: 이관 후 새 소유자

        // targetUsername이 지정된 경우, 해당 사용자의 "업무이관" 보드 찾기 또는 생성
        if (request.getTargetUsername() != null) {
            Board transferBoard = getOrCreateTransferBoard(request.getTargetUsername(), currentUsername);
            targetBoardId = transferBoard.getBoardId();
            targetBoardName = transferBoard.getBoardName();
            newOwnerUsername = request.getTargetUsername();  // v2.2.1: 이관 대상자가 새 소유자
        } else if (targetBoardId != null) {
            // 대상 보드 조회
            Board targetBoard = boardMapper.findById(targetBoardId)
                    .orElseThrow(() -> new BusinessException("이관 대상 보드를 찾을 수 없습니다."));
            targetBoardName = targetBoard.getBoardName();
            newOwnerUsername = targetBoard.getOwnerUsername();  // v2.2.1: 대상 보드 소유자가 새 소유자

            // 대상 보드 접근 권한 확인
            if (!boardMapper.hasAccess(targetBoardId, currentUsername)) {
                throw new BusinessException("이관 대상 보드에 접근 권한이 없습니다.");
            }
        } else {
            throw new BusinessException("이관 대상 보드 또는 사용자를 지정해주세요.");
        }

        // 같은 보드로 이관 불가
        if (targetBoardId.equals(item.getBoardId())) {
            throw new BusinessException("같은 보드로는 이관할 수 없습니다.");
        }

        Long originalBoardId = item.getBoardId();
        String originalBoardName = item.getBoardName();

        // 업무 이관 실행 - v2.2.1: 소유자 변경 및 하위 업무 함께 이관
        int updated = itemMapper.transferToBoard(itemId, targetBoardId, newOwnerUsername, originalBoardId, currentUsername);
        if (updated == 0) {
            throw new BusinessException("업무 이관에 실패했습니다.");
        }

        // 감사 로그 기록
        auditLogService.log(
                "ITEM", itemId, "TRANSFER",
                currentUsername, String.format("업무 이관: %s → %s", originalBoardName, targetBoardName),
                null, null, request.getTargetUsername()
        );

        log.info("Item {} transferred from board {} to board {} by user {}",
                itemId, originalBoardId, targetBoardId, currentUsername);

        // 이관된 업무 조회 후 반환
        Item transferredItem = itemMapper.findById(itemId)
                .orElseThrow(() -> new BusinessException("이관된 업무를 찾을 수 없습니다."));

        return ItemResponse.from(transferredItem);
    }

    /**
     * 사용자의 "업무이관" 보드 조회 또는 생성
     * - 이미 존재하면 해당 보드 반환
     * - 없으면 새로 생성하여 반환
     */
    private Board getOrCreateTransferBoard(String targetUsername, String currentUsername) {
        // 사용자의 "업무이관" 보드 조회
        return boardMapper.findByOwnerUsernameAndName(targetUsername, TRANSFER_BOARD_NAME)
                .orElseGet(() -> {
                    // 없으면 새로 생성
                    log.info("Creating transfer board for user {}", targetUsername);

                    // 최대 정렬 순서 조회
                    Integer maxSortOrder = boardMapper.getMaxSortOrder(targetUsername);
                    int newSortOrder = (maxSortOrder != null ? maxSortOrder : 0) + 1;

                    Board newBoard = Board.builder()
                            .boardName(TRANSFER_BOARD_NAME)
                            .description("다른 사용자로부터 이관받은 업무가 저장되는 보드입니다.")
                            .ownerUsername(targetUsername)
                            .defaultView("TABLE")
                            .sortOrder(newSortOrder)
                            .useYn("Y")
                            .createdBy(currentUsername)
                            .build();

                    boardMapper.insert(newBoard);

                    log.info("Transfer board created: boardId={} for user {}",
                            newBoard.getBoardId(), targetUsername);

                    return newBoard;
                });
    }

    /**
     * 이관 가능 여부 확인
     * - 업무 생성자
     * - FULL 권한 보유자 (업무 공유)
     * - 보드 소유자
     * - 보드에서 FULL 권한 보유자
     */
    @Transactional(readOnly = true)
    public boolean canTransfer(Long itemId, String username) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // v2.2.1: 업무 소유자인 경우 (기존: 생성자)
        if (username.equals(item.getOwnerUsername())) {
            return true;
        }

        // 보드 소유자인 경우
        Board board = boardMapper.findById(item.getBoardId()).orElse(null);
        if (board != null && username.equals(board.getOwnerUsername())) {
            return true;
        }

        // 보드에서 FULL 권한을 가진 경우
        String boardPermission = boardMapper.getUserPermission(item.getBoardId(), username);
        if ("FULL".equals(boardPermission)) {
            return true;
        }

        // 업무 공유에서 FULL 권한을 가진 경우
        String itemPermission = getPermission(itemId, username);
        return ItemShare.PERMISSION_FULL.equals(itemPermission);
    }

    /**
     * 공유 가능 여부 확인 (이관 권한과 동일)
     */
    @Transactional(readOnly = true)
    public boolean canShareItem(Long itemId, String username) {
        return canTransfer(itemId, username);
    }

    // =============================================
    // 확장된 접근 권한 확인 (보드 수준 포함)
    // =============================================

    /**
     * 업무 접근 권한 확인 (보드 수준 권한 포함)
     * - 업무 생성자
     * - 보드 소유자/공유자
     * - 업무 공유자
     */
    @Transactional(readOnly = true)
    public boolean hasItemAccess(Long itemId, String username) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // v2.2.1: 업무 소유자인 경우 (기존: 생성자)
        if (username.equals(item.getOwnerUsername())) {
            return true;
        }

        // 보드 접근 권한이 있는 경우
        if (boardMapper.hasAccess(item.getBoardId(), username)) {
            return true;
        }

        // 업무 공유받은 경우
        return itemShareMapper.existsByItemIdAndUsername(itemId, username);
    }

    /**
     * 업무 권한 조회 (보드 수준 권한 포함)
     * 권한 우선순위: OWNER > FULL > EDIT > VIEW
     */
    @Transactional(readOnly = true)
    public String getItemPermission(Long itemId, String username) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return null;
        }

        // v2.2.1: 업무 소유자인 경우 OWNER 권한 (기존: 생성자)
        if (username.equals(item.getOwnerUsername())) {
            return "OWNER";
        }

        // 보드 권한 확인
        Board board = boardMapper.findById(item.getBoardId()).orElse(null);
        if (board != null && username.equals(board.getOwnerUsername())) {
            return "OWNER";
        }

        String boardPermission = boardMapper.getUserPermission(item.getBoardId(), username);
        String itemPermission = getPermission(itemId, username);

        // 더 높은 권한 반환
        return getHigherPermission(boardPermission, itemPermission);
    }

    /**
     * 수정 권한 확인 (보드 수준 권한 포함)
     */
    @Transactional(readOnly = true)
    public boolean canEditItem(Long itemId, String username) {
        String permission = getItemPermission(itemId, username);
        if (permission == null) {
            return false;
        }
        return "OWNER".equals(permission) || "FULL".equals(permission) || "EDIT".equals(permission);
    }

    /**
     * 삭제 권한 확인 (보드 수준 권한 포함)
     */
    @Transactional(readOnly = true)
    public boolean canDeleteItem(Long itemId, String username) {
        String permission = getItemPermission(itemId, username);
        if (permission == null) {
            return false;
        }
        return "OWNER".equals(permission) || "FULL".equals(permission);
    }

    // =============================================
    // ItemShareResponse 형식 응답
    // =============================================

    /**
     * 업무 공유 목록 조회 (ItemShareResponse 형식)
     */
    @Transactional(readOnly = true)
    public List<ItemShareResponse> getItemShares(Long itemId) {
        return ItemShareResponse.fromList(itemShareMapper.selectByItemId(itemId));
    }

    /**
     * 더 높은 권한 반환
     */
    private String getHigherPermission(String perm1, String perm2) {
        int level1 = getPermissionLevel(perm1);
        int level2 = getPermissionLevel(perm2);
        return level1 >= level2 ? perm1 : perm2;
    }

    /**
     * 권한 레벨 (OWNER=4, FULL=3, EDIT=2, VIEW=1, null=0)
     */
    private int getPermissionLevel(String permission) {
        if (permission == null) return 0;
        switch (permission) {
            case "OWNER": return 4;
            case "FULL": return 3;
            case "EDIT": return 2;
            case "VIEW": return 1;
            default: return 0;
        }
    }
}
