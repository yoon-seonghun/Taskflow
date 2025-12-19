package com.taskflow.service.impl;

import com.taskflow.domain.ItemHistory;
import com.taskflow.domain.TaskTemplate;
import com.taskflow.dto.history.*;
import com.taskflow.mapper.ItemHistoryMapper;
import com.taskflow.mapper.TaskTemplateMapper;
import com.taskflow.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이력 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryServiceImpl implements HistoryService {

    private final ItemHistoryMapper itemHistoryMapper;
    private final TaskTemplateMapper taskTemplateMapper;

    @Override
    public ItemHistoryPageResponse getItemHistory(ItemHistorySearchRequest request) {
        log.debug("Get item history: request={}", request);

        // 이력 목록 조회
        List<ItemHistory> histories = itemHistoryMapper.findItemHistory(request);

        // 총 개수 조회
        long totalElements = itemHistoryMapper.countItemHistory(request);

        // 응답 변환
        List<ItemHistoryResponse> content = ItemHistoryResponse.fromList(histories);

        return ItemHistoryPageResponse.of(content, request.getPage(), request.getSize(), totalElements);
    }

    @Override
    public TemplateHistoryPageResponse getTemplateHistory(TemplateHistorySearchRequest request) {
        log.debug("Get template history: request={}", request);

        // 이력 목록 조회
        List<TaskTemplate> templates = taskTemplateMapper.findTemplateHistory(request);

        // 총 개수 조회
        long totalElements = taskTemplateMapper.countTemplateHistory(request);

        // 응답 변환
        List<TemplateHistoryResponse> content = TemplateHistoryResponse.fromList(templates);

        return TemplateHistoryPageResponse.of(content, request.getPage(), request.getSize(), totalElements);
    }
}
