package com.ticket.engine.engine;

import com.ticket.engine.dto.StateMachineDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class StateMachineCache {

    private static final String CACHE_PREFIX = "state_machine:";
    private static final long DEFAULT_EXPIRE_MINUTES = 60;

    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private long expireMinutes = DEFAULT_EXPIRE_MINUTES;

    public StateMachineDefinition get(Long tenantId, Long stateMachineId) {
        String key = buildKey(tenantId, stateMachineId);

        if (redisTemplate != null) {
            try {
                StateMachineDefinition definition = (StateMachineDefinition) redisTemplate.opsForValue().get(key);
                if (definition != null) {
                    return definition;
                }
            } catch (Exception e) {
                // Redis不可用时降级到本地缓存
            }
        }

        CacheEntry entry = localCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getDefinition();
        }

        if (entry != null && entry.isExpired()) {
            localCache.remove(key);
        }

        return null;
    }

    public void put(Long tenantId, Long stateMachineId, StateMachineDefinition definition) {
        String key = buildKey(tenantId, stateMachineId);
        long expireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(expireMinutes);

        localCache.put(key, new CacheEntry(definition, expireTime));

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, definition, expireMinutes, TimeUnit.MINUTES);
            } catch (Exception e) {
                // Redis不可用时忽略，仅使用本地缓存
            }
        }
    }

    public void evict(Long tenantId, Long stateMachineId) {
        String key = buildKey(tenantId, stateMachineId);
        localCache.remove(key);

        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                // Redis不可用时忽略
            }
        }
    }

    public void evictByTenant(Long tenantId) {
        String prefix = buildKey(tenantId, null);
        Iterator<Map.Entry<String, CacheEntry>> iterator = localCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            if (entry.getKey().startsWith(prefix)) {
                iterator.remove();
            }
        }

        if (redisTemplate != null) {
            try {
                String pattern = prefix + "*";
                var keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception e) {
                // Redis不可用时忽略
            }
        }
    }

    public void setExpireMinutes(long expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public void cleanExpired() {
        long now = System.currentTimeMillis();
        localCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private String buildKey(Long tenantId, Long stateMachineId) {
        if (stateMachineId == null) {
            return CACHE_PREFIX + tenantId + ":";
        }
        return CACHE_PREFIX + tenantId + ":" + stateMachineId;
    }

    private static class CacheEntry {
        private final StateMachineDefinition definition;
        private final long expireTime;

        public CacheEntry(StateMachineDefinition definition, long expireTime) {
            this.definition = definition;
            this.expireTime = expireTime;
        }

        public StateMachineDefinition getDefinition() {
            return definition;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long now) {
            return now > expireTime;
        }
    }
}
