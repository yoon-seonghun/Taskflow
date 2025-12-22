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
    public void addShare(Long itemId, ShareRequest request, Long currentUserId) {
        // 업무 존재 확인
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> new BusinessException("업무를 찾을 수 없습니다."));

        // 중복 확인
        if (itemShareMapper.existsByItemIdAndUserId(itemId, request.getUserId())) {
            throw new BusinessException("이미 공유된 사용자입니다.");
        }

        // 자기 자신에게 공유 불가
        if (request.getUserId().equals(item.getCreatedBy())) {
            throw new BusinessException("업무 생성자에게는 공유할 수 없습니다.");
        }

        ItemShare itemShare = ItemShare.builder()
                .itemId(itemId)
                .userId(request.getUserId())
                .permission(request.getPermission())
                .createdBy(currentUserId)
                .build();

        itemShareMapper.insert(itemShare);

        // 감사 로그 기록
        auditLogService.logItemShared(itemId, currentUserId, request.getUserId(), request.getPermission());

        log.info("Item {} shared with user {} (permission: {})", itemId, request.getUserId(), request.getPermission());
    }

    /**
     * 권한 변경
     */
    @Transactional
    public void updatePermission(Long itemId, Long userId, String permission, Long currentUserId) {
        // 기존 공유 확인
        ItemShare existing = itemShareMapper.selectByItemIdAndUserId(itemId, userId);
        if (existing == null) {
            throw new BusinessException("공유 정보를 찾을 수 없습니다.");
        }

        String oldPermission = existing.getPermission();
        itemShareMapper.updatePermission(itemId, userId, permission, currentUserId);

        // 감사 로그 기록
        auditLogService.log(
                "ITEM_SHARE", itemId, "UPDATE",
                currentUserId, String.format("권한 변경: %s → %s", oldPermission, permission),
                null, null, userId
        );

        log.info("Item {} share permission changed for user {} ({} -> {})",
                itemId, userId, oldPermission, permission);
    }

    /**
     * 공유 제거
     */
    @Transactional
    public void removeShare(Long itemId, Long userId, Long currentUserId) {
        // 기존 공유 확인
        if (!itemShareMapper.existsByItemIdAndUserId(itemId, userId)) {
            throw new BusinessException("공유 정보를 찾을 수 없습니다.");
        }

        itemShareMapper.delete(itemId, userId);

        // 감사 로그 기록
        auditLogService.logItemUnshared(itemId, currentUserId, userId);

        log.info("Item {} share removed for user {}", itemId, userId);
    }

    /**
     * 사용자의 업무 접근 권한 확인
     */
    @Transactional(readOnly = true)
    public String getPermission(Long itemId, Long userId) {
        return itemShareMapper.getPermission(itemId, userId);
    }

    /**
     * 사용자가 업무에 접근 가능한지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(Long itemId, Long userId) {
        // 업무 조회
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // 생성자인 경우
        if (userId.equals(item.getCreatedBy())) {
            return true;
        }

        // 공유받은 경우
        return itemShareMapper.existsByItemIdAndUserId(itemId, userId);
    }

    /**
     * 수정 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean canEdit(Long itemId, Long userId) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // 생성자인 경우
        if (userId.equals(item.getCreatedBy())) {
            return true;
        }

        // 공유 권한 확인
        String permission = getPermission(itemId, userId);
        return ItemShare.PERMISSION_EDIT.equals(permission) || ItemShare.PERMISSION_FULL.equals(permission);
    }

    /**
     * 삭제 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean canDelete(Long itemId, Long userId) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // 생성자인 경우
        if (userId.equals(item.getCreatedBy())) {
            return true;
        }

        // 공유 권한 확인
        String permission = getPermission(itemId, userId);
        return ItemShare.PERMISSION_FULL.equals(permission);
    }

    private ShareResponse toResponse(ItemShare share) {
        return ShareResponse.builder()
                .userId(share.getUserId())
                .loginId(share.getLoginId())
                .userName(share.getUserName())
                .departmentName(share.getDepartmentName())
                .permission(share.getPermission())
                .createdAt(share.getCreatedAt())
                .build();
    }

    // =============================================
    // 개별 업무 이관 기능
    // =============================================

    /**
     * 개별 업무 이관
     * - 다른 보드로 이관: targetBoardId 사용
     * - 다른 사용자에게 이관: targetUserId 사용 (사용자의 기본 보드로 이관)
     */
    @Transactional
    public ItemResponse transferItem(Long itemId, ItemTransferRequest request, Long currentUserId) {
        // 업무 조회
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> new BusinessException("업무를 찾을 수 없습니다."));

        // 이관 권한 확인
        if (!canTransfer(itemId, currentUserId)) {
            throw new BusinessException("업무를 이관할 권한이 없습니다.");
        }

        Long targetBoardId = request.getTargetBoardId();
        String targetBoardName = null;

        // targetUserId가 지정된 경우, 해당 사용자의 기본 보드 찾기
        if (request.getTargetUserId() != null) {
            List<Board> userBoards = boardMapper.findByOwnerId(request.getTargetUserId(), "Y");
            if (userBoards.isEmpty()) {
                throw new BusinessException("이관 대상 사용자의 보드가 없습니다.");
            }
            // 정렬 순서가 가장 낮은 보드 (기본 보드)
            targetBoardId = userBoards.get(0).getBoardId();
            targetBoardName = userBoards.get(0).getBoardName();
        } else if (targetBoardId != null) {
            // 대상 보드 조회
            Board targetBoard = boardMapper.findById(targetBoardId)
                    .orElseThrow(() -> new BusinessException("이관 대상 보드를 찾을 수 없습니다."));
            targetBoardName = targetBoard.getBoardName();

            // 대상 보드 접근 권한 확인
            if (!boardMapper.hasAccess(targetBoardId, currentUserId)) {
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

        // 업무 이관 실행
        int updated = itemMapper.transferToBoard(itemId, targetBoardId, originalBoardId, currentUserId);
        if (updated == 0) {
            throw new BusinessException("업무 이관에 실패했습니다.");
        }

        // 감사 로그 기록
        auditLogService.log(
                "ITEM", itemId, "TRANSFER",
                currentUserId, String.format("업무 이관: %s → %s", originalBoardName, targetBoardName),
                null, null, request.getTargetUserId()
        );

        log.info("Item {} transferred from board {} to board {} by user {}",
                itemId, originalBoardId, targetBoardId, currentUserId);

        // 이관된 업무 조회 후 반환
        Item transferredItem = itemMapper.findById(itemId)
                .orElseThrow(() -> new BusinessException("이관된 업무를 찾을 수 없습니다."));

        return ItemResponse.from(transferredItem);
    }

    /**
     * 이관 가능 여부 확인
     * - 업무 생성자
     * - FULL 권한 보유자 (업무 공유)
     * - 보드 소유자
     * - 보드에서 FULL 권한 보유자
     */
    @Transactional(readOnly = true)
    public boolean canTransfer(Long itemId, Long userId) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // 업무 생성자인 경우
        if (userId.equals(item.getCreatedBy())) {
            return true;
        }

        // 보드 소유자인 경우
        Board board = boardMapper.findById(item.getBoardId()).orElse(null);
        if (board != null && userId.equals(board.getOwnerId())) {
            return true;
        }

        // 보드에서 FULL 권한을 가진 경우
        String boardPermission = boardMapper.getUserPermission(item.getBoardId(), userId);
        if ("FULL".equals(boardPermission)) {
            return true;
        }

        // 업무 공유에서 FULL 권한을 가진 경우
        String itemPermission = getPermission(itemId, userId);
        return ItemShare.PERMISSION_FULL.equals(itemPermission);
    }

    /**
     * 공유 가능 여부 확인 (이관 권한과 동일)
     */
    @Transactional(readOnly = true)
    public boolean canShareItem(Long itemId, Long userId) {
        return canTransfer(itemId, userId);
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
    public boolean hasItemAccess(Long itemId, Long userId) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return false;
        }

        // 업무 생성자인 경우
        if (userId.equals(item.getCreatedBy())) {
            return true;
        }

        // 보드 접근 권한이 있는 경우
        if (boardMapper.hasAccess(item.getBoardId(), userId)) {
            return true;
        }

        // 업무 공유받은 경우
        return itemShareMapper.existsByItemIdAndUserId(itemId, userId);
    }

    /**
     * 업무 권한 조회 (보드 수준 권한 포함)
     * 권한 우선순위: OWNER > FULL > EDIT > VIEW
     */
    @Transactional(readOnly = true)
    public String getItemPermission(Long itemId, Long userId) {
        Item item = itemMapper.findById(itemId).orElse(null);
        if (item == null) {
            return null;
        }

        // 업무 생성자인 경우 OWNER 권한
        if (userId.equals(item.getCreatedBy())) {
            return "OWNER";
        }

        // 보드 권한 확인
        Board board = boardMapper.findById(item.getBoardId()).orElse(null);
        if (board != null && userId.equals(board.getOwnerId())) {
            return "OWNER";
        }

        String boardPermission = boardMapper.getUserPermission(item.getBoardId(), userId);
        String itemPermission = getPermission(itemId, userId);

        // 더 높은 권한 반환
        return getHigherPermission(boardPermission, itemPermission);
    }

    /**
     * 수정 권한 확인 (보드 수준 권한 포함)
     */
    @Transactional(readOnly = true)
    public boolean canEditItem(Long itemId, Long userId) {
        String permission = getItemPermission(itemId, userId);
        if (permission == null) {
            return false;
        }
        return "OWNER".equals(permission) || "FULL".equals(permission) || "EDIT".equals(permission);
    }

    /**
     * 삭제 권한 확인 (보드 수준 권한 포함)
     */
    @Transactional(readOnly = true)
    public boolean canDeleteItem(Long itemId, Long userId) {
        String permission = getItemPermission(itemId, userId);
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
