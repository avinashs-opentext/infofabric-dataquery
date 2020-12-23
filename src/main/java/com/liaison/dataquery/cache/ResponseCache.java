/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.inject.Inject;
import com.liaison.dataquery.DataqueryConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ResponseCache {

    private ConcurrentHashMap<String, Cache<Object, Object>> caches = new ConcurrentHashMap<>();
    private final Long MAX_ENTRY_BYTES;
    private final Long TTL_MS;
    private final Long MAX_SIZE;
    private final boolean RECORD_STATS;
    private static final Logger log = LoggerFactory.getLogger(ResponseCache.class);

    @Inject
    public ResponseCache(DataqueryConfiguration configuration) {
        MAX_ENTRY_BYTES = configuration.getMaxCacheEntryBytes();
        TTL_MS = configuration.getCacheTTLMs();
        MAX_SIZE = configuration.getCacheSize();
        RECORD_STATS = configuration.getRecordCacheStats();
        log.info("Response cache configuration: Max entry size={}B, Entries per cache={}, TTL={}ms, Record stats={}",
                MAX_ENTRY_BYTES, MAX_SIZE, TTL_MS, RECORD_STATS);
    }

    public void put(String cacheId, Object key, byte[] value) {
        if (key == null || value == null || StringUtils.isEmpty(cacheId)) {
            return;
        }
        if (value.length > MAX_ENTRY_BYTES) {
            log.warn("Cache entry larger than maximum allowed {} > {} bytes. Result not cached.", value.length, MAX_ENTRY_BYTES);
            return;
        }
        if (!caches.containsKey(cacheId)) {
            caches.put(cacheId, buildCache());
        }
        caches.get(cacheId).put(key, value);
    }

    public byte[] get(String cacheId, Object key) {
        if (StringUtils.isEmpty(cacheId) || key == null) {
            return null;
        }
        if (caches.containsKey(cacheId)) {
            return (byte[]) caches.get(cacheId).getIfPresent(key);
        }
        return null;
    }

    public static String getCacheIdentifier(String tenant, String model, String view) {
        return String.join("-", tenant.toLowerCase(), model.toLowerCase(), view.toLowerCase());
    }

    public static String getCacheIdentifier(String tenant, String model, String view, String token) {
        return String.join("-", getCacheIdentifier(tenant, model, view), token);
    }

    private Cache<Object, Object> buildCache() {
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterWrite(TTL_MS, TimeUnit.MILLISECONDS);
        if (RECORD_STATS) {
            cacheBuilder.recordStats();
        }
        return cacheBuilder.build();
    }

    public Map<String, CacheStats> getStats() {
        Map<String, CacheStats> stats = new HashMap<>();
        caches.forEach((k, v) -> stats.put(k, v.stats()));
        return stats;
    }
}
