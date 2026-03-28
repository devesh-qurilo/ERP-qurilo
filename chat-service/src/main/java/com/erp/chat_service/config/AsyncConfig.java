package com.erp.chat_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("chatAsyncExecutor")
    public Executor chatAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("chat-async-");
        ex.setTaskDecorator(contextCopyingDecorator());
        ex.initialize();
        return ex;
    }

    private TaskDecorator contextCopyingDecorator() {
        return runnable -> {
            // Copy RequestAttributes
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            // Copy SecurityContext
            var securityContext = SecurityContextHolder.getContext();

            return () -> {
                try {
                    if (requestAttributes != null) {
                        RequestContextHolder.setRequestAttributes(requestAttributes, true);
                    }
                    if (securityContext != null) {
                        SecurityContextHolder.setContext(securityContext);
                    }
                    runnable.run();
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.clearContext();
                }
            };
        };
    }
}
