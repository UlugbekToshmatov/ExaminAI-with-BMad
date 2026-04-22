package com.examinai.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(AsyncConfig.class)
class AsyncConfigTest {

    @Autowired
    private AsyncConfigurer asyncConfigurer;

    @Test
    void asyncExecutorUsesCallerRunsWithWarningHandler() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfigurer.getAsyncExecutor();
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
            .isInstanceOf(CallerRunsWithWarningHandler.class);
    }
}
