package com.examinai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/** {@link ThreadPoolExecutor.CallerRunsPolicy} plus a WARN when the pool is saturated. */
public final class CallerRunsWithWarningHandler implements RejectedExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(CallerRunsWithWarningHandler.class);

    private final RejectedExecutionHandler delegate = new ThreadPoolExecutor.CallerRunsPolicy();

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        log.warn(
            "Async executor saturated — activeThreads={}, poolSize={}, queueSize={}; running task on caller thread",
            executor.getActiveCount(),
            executor.getPoolSize(),
            executor.getQueue().size());
        delegate.rejectedExecution(r, executor);
    }
}
