package dev.ekzo.camunda.connector.inbound.graphql.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedExecutorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedExecutorService.class);

    private static final SharedExecutorService INSTANCE = new SharedExecutorService();
    private static final int THREAD_POOL_SIZE = 10;

    private final ScheduledExecutorService executorService;

    private SharedExecutorService() {
        this.executorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
        addShutdownHook();
    }

    public static SharedExecutorService getInstance() {
        return INSTANCE;
    }

    private void addShutdownHook() {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    executorService.shutdownNow();
                                    LOGGER.info("Shutdown hook activated, terminating executor service.");
                                    Thread.currentThread().interrupt();
                                }));
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }
}
