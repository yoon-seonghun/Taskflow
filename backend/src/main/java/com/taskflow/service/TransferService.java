package com.taskflow.service;

import com.taskflow.domain.Board;
import com.taskflow.dto.transfer.TransferPreviewResponse;
import com.taskflow.dto.transfer.TransferRequest;
import com.taskflow.dto.transfer.TransferResultResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.BoardMapper;
import com.taskflow.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 업무 이관 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final BoardMapper boardMapper;
    private final ItemMapper itemMapper;
    private final AuditLogService auditLogService;

    /**
     * 이관 대상 업무 미리보기
     */
    @Transactional(readOnly = true)
    public TransferPreviewResponse getTransferPreview(Long boardId) {
        // 보드 조회
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> new BusinessException("보드를 찾을 수 없습니다."));

        // 미완료 업무 목록 조회
        List<Map<String, Object>> pendingItems = boardMapper.findPendingItems(boardId);

        List<TransferPreviewResponse.PendingItem> items = pendingItems.stream()
                .map(this::toPendingItem)
                .collect(Collectors.toList());

        return TransferPreviewResponse.builder()
                .boardId(boardId)
                .boardName(board.getBoardName())
                .pendingItems(items)
                .totalCount(items.size())
                .build();
    }

    /**
     * 업무 이관 실행
     */
    @Transactional
    public TransferResultResponse executeTransfer(Long boardId, TransferRequest request, Long currentUserId) {
        // 원본 보드 조회
        Board originalBoard = boardMapper.findById(boardId)
                .orElseThrow(() -> new BusinessException("보드를 찾을 수 없습니다."));

        // 소유자 확인
        if (!originalBoard.getOwnerId().equals(currentUserId)) {
            throw new BusinessException("보드 소유자만 업무를 이관할 수 있습니다.");
        }

        // 이관 대상 사용자 확인
        if (request.getTargetUserId() == null) {
            throw new BusinessException("이관 대상자를 지정해주세요.");
        }

        // 이관 보드 생성
        String newBoardName = createTransferBoardName(originalBoard.getBoardName());
        Board newBoard = createTransferBoard(request.getTargetUserId(), newBoardName, currentUserId);

        // 업무 이동
        List<Long> itemIds = request.getItemIds();
        int transferredCount = itemMapper.transferItemsToBoard(
                itemIds,
                newBoard.getBoardId(),
                boardId,
                currentUserId
        );

        // 각 업무에 대한 이관 로그 기록
        for (Long itemId : itemIds) {
            auditLogService.logItemTransferred(
                    itemId,
                    currentUserId,
                    request.getTargetUserId(),
                    originalBoard.getBoardName(),
                    newBoardName
            );
        }

        log.info("Transferred {} items from board {} to new board {} for user {}",
                transferredCount, boardId, newBoard.getBoardId(), request.getTargetUserId());

        return TransferResultResponse.builder()
                .transferredCount(transferredCount)
                .newBoardId(newBoard.getBoardId())
                .newBoardName(newBoardName)
                .message(transferredCount + "건의 업무가 이관되었습니다.")
                .build();
    }

    /**
     * 이관 보드 생성
     */
    private Board createTransferBoard(Long targetUserId, String boardName, Long createdBy) {
        // 최대 정렬 순서 조회
        Integer maxSortOrder = boardMapper.getMaxSortOrder(targetUserId);

        Board board = Board.builder()
                .boardName(boardName)
                .description("이관받은 업무 보드")
                .ownerId(targetUserId)
                .defaultView("TABLE")
                .color("#FF9800")  // 오렌지 색상으로 구분
                .sortOrder(maxSortOrder + 1)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        boardMapper.insert(board);

        // 감사 로그 기록
        auditLogService.logBoardCreated(board.getBoardId(), createdBy, boardName);

        return board;
    }

    /**
     * 이관 보드명 생성
     */
    private String createTransferBoardName(String originalBoardName) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return "[이관] " + originalBoardName + " - " + date;
    }

    private TransferPreviewResponse.PendingItem toPendingItem(Map<String, Object> map) {
        return TransferPreviewResponse.PendingItem.builder()
                .itemId(((Number) map.get("ITEM_ID")).longValue())
                .title((String) map.get("TITLE"))
                .status((String) map.get("STATUS"))
                .priority((String) map.get("PRIORITY"))
                .assigneeName((String) map.get("ASSIGNEE_NAME"))
                .build();
    }
}
