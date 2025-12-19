package com.taskflow.service.impl;

import com.taskflow.domain.Board;
import com.taskflow.domain.BoardShare;
import com.taskflow.domain.Group;
import com.taskflow.domain.User;
import com.taskflow.dto.board.*;
import com.taskflow.dto.group.GroupResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.BoardMapper;
import com.taskflow.mapper.BoardShareMapper;
import com.taskflow.mapper.GroupMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
        return BoardResponse.fromList(boards);
    }

    @Override
    public List<BoardResponse> getOwnedBoards(Long ownerId, String useYn) {
        List<Board> boards = boardMapper.findByOwnerId(ownerId, useYn);
        return BoardResponse.fromList(boards);
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

        return getBoard(board.getBoardId());
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

        return getBoard(boardId);
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
}
