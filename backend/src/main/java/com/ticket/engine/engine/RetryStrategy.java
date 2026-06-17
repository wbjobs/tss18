package com.ticket.engine.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Component
public class RetryStrategy {

    private static final Logger log = LoggerFactory.getLogger(RetryStrategy.class);

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_INTERVAL_MILLIS = 1000;
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final long DEFAULT_MAX_INTERVAL_MILLIS = 30000;

    public <T> T executeWithRetry(Callable<T> task, String taskName) {
        return executeWithRetry(task, taskName, DEFAULT_MAX_RETRIES, null);
    }

    public <T> T executeWithRetry(Callable<T> task, String taskName,
                                   int maxRetries, Predicate<Exception> retryablePredicate) {
        return executeWithRetry(task, taskName, maxRetries,
            DEFAULT_INITIAL_INTERVAL_MILLIS, DEFAULT_MULTIPLIER,
            DEFAULT_MAX_INTERVAL_MILLIS, retryablePredicate);
    }

    public <T> T executeWithRetry(Callable<T> task, String taskName,
                                   int maxRetries, long initialIntervalMillis,
                                   double multiplier, long maxIntervalMillis,
                                   Predicate<Exception> retryablePredicate) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                T result = task.call();
                if (attempt > 0) {
                    log.info("重试成功: task={}, attempt={}", taskName, attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;

                if (attempt >= maxRetries) {
                    break;
                }

                if (retryablePredicate != null && !retryablePredicate.test(e)) {
                    log.warn("不可重试异常，放弃重试: task={}, attempt={}: {}",
                        taskName, attempt, e.getMessage());
                    break;
                }

                long delay = calculateDelay(initialIntervalMillis, multiplier, maxIntervalMillis, attempt);

                log.warn("执行失败，准备重试: task={}, attempt={}/{}, nextDelay={}ms, error={}",
                    taskName, attempt + 1, maxRetries, delay, e.getMessage());

                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断: " + taskName, ie);
                }
            }
        }

        throw new RuntimeException(
            String.format("重试耗尽: task=%s, maxRetries=%d, lastError=%s",
                taskName, maxRetries,
                lastException != null ? lastException.getMessage() : "unknown"),
            lastException);
    }

    public void executeWithRetry(Runnable task, String taskName) {
        executeWithRetry(() -> {
            task.run();
            return null;
        }, taskName);
    }

    public void executeWithRetry(Runnable task, String taskName,
                                  int maxRetries, Predicate<Exception> retryablePredicate) {
        executeWithRetry(() -> {
            task.run();
            return null;
        }, taskName, maxRetries, retryablePredicate);
    }

    private long calculateDelay(long initialInterval, double multiplier,
                                 long maxInterval, int attempt) {
        long delay = (long) (initialInterval * Math.pow(multiplier, attempt));
        return Math.min(delay, maxInterval);
    }

    public static Predicate<Exception> isRetryableOn(Class<? extends Exception>... exceptionTypes) {
        return e -> {
            for (Class<? extends Exception> type : exceptionTypes) {
                if (type.isInstance(e)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predicate<Exception> isRetryableOnTimeout() {
        return e -> {
            String className = e.getClass().getName();
            return className.contains("TimeoutException")
                || className.contains("SocketTimeoutException")
                || className.contains("ConnectTimeoutException")
                || className.contains("ConnectException")
                || e.getMessage() != null && e.getMessage().contains("timeout");
        };
    }

    public static Predicate<Exception> retryAll() {
        return e -> true;
    }
}
