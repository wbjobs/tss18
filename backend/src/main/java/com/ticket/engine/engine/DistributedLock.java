package com.ticket.engine.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(DistributedLock.class);

    private static final String LOCK_PREFIX = "ticket_lock:";
    private static final long DEFAULT_LOCK_SECONDS = 30;
    private static final long DEFAULT_WAIT_MILLIS = 200;
    private static final long DEFAULT_MAX_WAIT_MILLIS = 10000;

    private static final String UNLOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "  return redis.call('del', KEYS[1]) " +
        "else " +
        "  return 0 " +
        "end";

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    public LockHolder tryLock(Long ticketId) {
        return tryLock(ticketId, DEFAULT_LOCK_SECONDS, DEFAULT_MAX_WAIT_MILLIS);
    }

    public LockHolder tryLock(Long ticketId, long lockSeconds, long maxWaitMillis) {
        String lockKey = LOCK_PREFIX + ticketId;
        String lockValue = UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();

        if (redisTemplate == null) {
            log.warn("Redis不可用，分布式锁降级为本地同步锁, ticketId={}", ticketId);
            return LockHolder.localLock(ticketId);
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            try {
                Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, lockSeconds, TimeUnit.SECONDS);

                if (Boolean.TRUE.equals(acquired)) {
                    log.debug("获取分布式锁成功: ticketId={}, lockValue={}", ticketId, lockValue);
                    return LockHolder.distributedLock(ticketId, lockKey, lockValue, this);
                }
            } catch (Exception e) {
                log.warn("Redis操作异常，降级为本地同步锁, ticketId={}: {}", ticketId, e.getMessage());
                return LockHolder.localLock(ticketId);
            }

            try {
                Thread.sleep(DEFAULT_WAIT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return LockHolder.failed(ticketId);
            }
        }

        log.warn("获取分布式锁超时: ticketId={}, maxWait={}ms", ticketId, maxWaitMillis);
        return LockHolder.failed(ticketId);
    }

    public void unlock(String lockKey, String lockValue) {
        if (redisTemplate == null) {
            return;
        }

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script,
                Collections.singletonList(lockKey), lockValue);

            if (result != null && result == 1L) {
                log.debug("释放分布式锁成功: lockKey={}", lockKey);
            } else {
                log.warn("释放分布式锁失败(锁已被其他线程持有或已过期): lockKey={}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: lockKey={}", lockKey, e);
        }
    }

    public static class LockHolder implements AutoCloseable {

        private final boolean acquired;
        private final Long ticketId;
        private final String lockKey;
        private final String lockValue;
        private final DistributedLock lockProvider;
        private final boolean distributed;
        private final Object localMonitor;

        private LockHolder(boolean acquired, Long ticketId, String lockKey,
                           String lockValue, DistributedLock lockProvider,
                           boolean distributed, Object localMonitor) {
            this.acquired = acquired;
            this.ticketId = ticketId;
            this.lockKey = lockKey;
            this.lockValue = lockValue;
            this.lockProvider = lockProvider;
            this.distributed = distributed;
            this.localMonitor = localMonitor;
        }

        static LockHolder distributedLock(Long ticketId, String lockKey,
                                           String lockValue, DistributedLock lockProvider) {
            return new LockHolder(true, ticketId, lockKey, lockValue, lockProvider, true, null);
        }

        static LockHolder localLock(Long ticketId) {
            return new LockHolder(true, ticketId, null, null, null, false, new Object());
        }

        static LockHolder failed(Long ticketId) {
            return new LockHolder(false, ticketId, null, null, null, false, null);
        }

        public boolean isAcquired() {
            return acquired;
        }

        public Long getTicketId() {
            return ticketId;
        }

        public Object getLocalMonitor() {
            return localMonitor;
        }

        @Override
        public void close() {
            if (!acquired) {
                return;
            }

            if (distributed && lockProvider != null && lockKey != null) {
                lockProvider.unlock(lockKey, lockValue);
            }
        }
    }
}
