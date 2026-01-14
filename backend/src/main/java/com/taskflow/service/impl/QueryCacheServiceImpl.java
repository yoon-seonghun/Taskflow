package com.taskflow.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.taskflow.dto.external.QueryExecuteResponse.QueryOption;
import com.taskflow.service.QueryCacheService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 외부 쿼리 결과 캐싱 서비스 구현체
 *
 * Caffeine 기반 TTL 캐싱을 제공합니다.
 * 각 쿼리별로 다른 TTL을 지원합니다.
 */
@Slf4j
@Service
public class QueryCacheServiceImpl implements QueryCacheService {

    @Value("${external-query.default-cache-ttl:300}")
    private int defaultCacheTtl;

    /**
     * 캐시 엔트리: 옵션 목록 + 만료 시간
     */
    private record CacheEntry(List<QueryOption> options, long expireAtNanos) {}

    /**
     * 쿼리별 TTL 저장소
     */
    private final ConcurrentHashMap<Long, Integer> ttlMap = new ConcurrentHashMap<>();

    /**
     * Caffeine 캐시
     */
    private Cache<Long, CacheEntry> cache;

    /**
     * 통계
     */
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    @PostConstruct
    public void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfter(new Expiry<Long, CacheEntry>() {
                    @Override
                    public long expireAfterCreate(Long key, CacheEntry value, long currentTime) {
                        // CacheEntry에 저장된 만료 시간 사용
                        return value.expireAtNanos() - currentTime;
                    }

                    @Override
                    public long expireAfterUpdate(Long key, CacheEntry value, long currentTime, @NonNegative long currentDuration) {
                        return value.expireAtNanos() - currentTime;
                    }

                    @Override
                    public long expireAfterRead(Long key, CacheEntry value, long currentTime, @NonNegative long currentDuration) {
                        return currentDuration;
                    }
                })
                .recordStats()
                .build();

        log.info("QueryCacheService initialized with default TTL: {}s", defaultCacheTtl);
    }

    @Override
    public Optional<List<QueryOption>> get(Long queryId) {
        CacheEntry entry = cache.getIfPresent(queryId);
        if (entry != null) {
            hitCount.incrementAndGet();
            log.debug("Cache hit for query: {}", queryId);
            return Optional.of(entry.options());
        }
        missCount.incrementAndGet();
        log.debug("Cache miss for query: {}", queryId);
        return Optional.empty();
    }

    @Override
    public void put(Long queryId, List<QueryOption> options, int ttlSeconds) {
        int effectiveTtl = ttlSeconds > 0 ? ttlSeconds : defaultCacheTtl;
        long expireAtNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(effectiveTtl);

        CacheEntry entry = new CacheEntry(options, expireAtNanos);
        cache.put(queryId, entry);
        ttlMap.put(queryId, effectiveTtl);

        log.debug("Cached query {} with TTL: {}s, options count: {}",
                queryId, effectiveTtl, options.size());
    }

    @Override
    public void evict(Long queryId) {
        cache.invalidate(queryId);
        ttlMap.remove(queryId);
        log.debug("Cache evicted for query: {}", queryId);
    }

    @Override
    public void evictByDatasource(Long datasourceId) {
        // 현재 구현에서는 datasource-query 매핑을 알 수 없으므로
        // 상위 서비스에서 queryId 목록을 조회하여 개별 evict 호출 필요
        log.debug("Evict by datasource requested: {}", datasourceId);
    }

    @Override
    public void evictAll() {
        cache.invalidateAll();
        ttlMap.clear();
        hitCount.set(0);
        missCount.set(0);
        log.info("All cache entries evicted");
    }

    @Override
    public boolean contains(Long queryId) {
        return cache.getIfPresent(queryId) != null;
    }

    @Override
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.stats();
        long hits = hitCount.get();
        long misses = missCount.get();
        double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;

        return new CacheStats(
                cache.estimatedSize(),
                hits,
                misses,
                hitRate
        );
    }
}
