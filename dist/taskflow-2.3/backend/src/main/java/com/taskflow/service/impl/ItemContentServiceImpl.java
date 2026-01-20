package com.taskflow.service.impl;

import com.taskflow.domain.ItemContent;
import com.taskflow.dto.content.ItemContentResponse;
import com.taskflow.dto.content.ItemContentUpdateRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.BoardMapper;
import com.taskflow.mapper.ItemContentMapper;
import com.taskflow.mapper.ItemMapper;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.ItemContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업무 상세 내용 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemContentServiceImpl implements ItemContentService {

    private final ItemContentMapper itemContentMapper;
    private final ItemMapper itemMapper;
    private final BoardMapper boardMapper;

    @Override
    public ItemContentResponse getContent(Long boardId, Long itemId) {
        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 아이템 존재 확인
        itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        // 내용 조회
        ItemContent content = itemContentMapper.findByItemId(itemId);
        return ItemContentResponse.from(content);
    }

    @Override
    @Transactional
    public ItemContentResponse updateContent(Long boardId, Long itemId, ItemContentUpdateRequest request) {
        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 아이템 존재 확인
        itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        String username = SecurityUtils.getCurrentUsername();
        String plainText = extractPlainText(request.getContent(), request.getContentType());

        // 내용 존재 여부 확인
        boolean exists = itemContentMapper.existsByItemId(itemId);

        if (exists) {
            // 수정 (버전 충돌 체크)
            int updated = itemContentMapper.update(
                    itemId,
                    request.getContentType(),
                    request.getContent(),
                    plainText,
                    request.getVersion(),
                    username
            );

            if (updated == 0) {
                // 버전 충돌
                throw BusinessException.versionConflict(
                        "다른 사용자가 수정했습니다. 새로고침 후 다시 시도해주세요."
                );
            }
        } else {
            // 신규 등록
            ItemContent content = ItemContent.builder()
                    .itemId(itemId)
                    .contentType(request.getContentType())
                    .content(request.getContent())
                    .plainText(plainText)
                    .createdBy(username)
                    .build();

            itemContentMapper.insert(content);
        }

        // 저장된 내용 조회 후 반환
        ItemContent saved = itemContentMapper.findByItemId(itemId);
        return ItemContentResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteContent(Long itemId) {
        itemContentMapper.deleteByItemId(itemId);
    }

    @Override
    public boolean hasContent(Long itemId) {
        return itemContentMapper.existsByItemId(itemId);
    }

    /**
     * HTML/JSON에서 평문 텍스트 추출
     */
    private String extractPlainText(String content, String contentType) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        if ("HTML".equalsIgnoreCase(contentType)) {
            // Jsoup으로 HTML 태그 제거
            return Jsoup.parse(content).text();
        } else if ("MARKDOWN".equalsIgnoreCase(contentType)) {
            // 마크다운 기본 문법 제거 (간단 처리)
            return content
                    .replaceAll("#+\\s*", "")  // 헤딩
                    .replaceAll("\\*+", "")     // 강조
                    .replaceAll("_+", "")       // 이탤릭
                    .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1")  // 링크
                    .replaceAll("`+", "")       // 코드
                    .replaceAll("^[-*+]\\s+", "")  // 목록
                    .trim();
        }

        // JSON 또는 기타: 그대로 반환 (또는 별도 파싱 필요)
        return content;
    }
}
