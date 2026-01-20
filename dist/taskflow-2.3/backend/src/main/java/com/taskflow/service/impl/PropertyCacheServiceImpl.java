package com.taskflow.service.impl;

import com.taskflow.domain.PropertyDef;
import com.taskflow.mapper.PropertyDefMapper;
import com.taskflow.service.PropertyCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 속성 정의 캐시 서비스 구현
 *
 * ConcurrentHashMap 기반의 인메모리 캐시
 * - 보드별로 속성 정의 목록 캐싱
 * - 속성/옵션 변경 시 해당 보드 캐시 무효화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyCacheServiceImpl implements PropertyCacheService {

    private final PropertyDefMapper propertyDefMapper;

    /**
     * 보드별 속성 정의 캐시
     * Key: boardId, Value: 속성 정의 목록 (옵션 포함)
     */
    private final Map<Long, List<PropertyDef>> propertyCache = new ConcurrentHashMap<>();

    @Override
    public List<PropertyDef> getPropertiesByBoardId(Long boardId) {
        // 캐시에서 조회
        List<PropertyDef> cached = propertyCache.get(boardId);
        if (cached != null) {
            log.debug("Property cache hit: boardId={}", boardId);
            return cached;
        }

        // 캐시 미스: DB에서 조회 후 캐싱
        log.debug("Property cache miss: boardId={}", boardId);
        List<PropertyDef> properties = propertyDefMapper.findByBoardIdWithOptions(boardId, "Y");
        propertyCache.put(boardId, properties);

        return properties;
    }

    @Override
    public void evictBoardCache(Long boardId) {
        log.info("Evicting property cache: boardId={}", boardId);
        propertyCache.remove(boardId);
    }

    @Override
    public void evictAllCache() {
        log.info("Evicting all property cache");
        propertyCache.clear();
    }

    @Override
    public void refreshBoardCache(Long boardId) {
        log.info("Refreshing property cache: boardId={}", boardId);
        List<PropertyDef> properties = propertyDefMapper.findByBoardIdWithOptions(boardId, "Y");
        propertyCache.put(boardId, properties);
    }
}
