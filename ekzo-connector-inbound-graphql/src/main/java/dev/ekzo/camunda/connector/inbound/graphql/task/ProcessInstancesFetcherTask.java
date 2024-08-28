/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package dev.ekzo.camunda.connector.inbound.graphql.task;

import dev.ekzo.camunda.connector.inbound.graphql.model.PollingIntervalConfiguration;
import io.camunda.connector.api.inbound.InboundIntermediateConnectorContext;
import io.camunda.connector.api.inbound.ProcessInstanceContext;
import dev.ekzo.camunda.connector.inbound.graphql.service.SharedExecutorService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessInstancesFetcherTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstancesFetcherTask.class);

    private final InboundIntermediateConnectorContext context;
    private final SharedExecutorService executorService;
    private final PollingIntervalConfiguration config;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> runningGraphqlRequestTaskIds;

    public ProcessInstancesFetcherTask(
            final InboundIntermediateConnectorContext context,
            final SharedExecutorService executorService) {
        LOGGER.debug("new {}()", getClass().getSimpleName());
        this.config = context.bindProperties(PollingIntervalConfiguration.class);
        this.context = context;
        this.executorService = executorService;
        this.runningGraphqlRequestTaskIds = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        LOGGER.debug("{}.run()", getClass().getSimpleName());
        try {
            List<ProcessInstanceContext> processInstanceContexts = context.getProcessInstanceContexts();
            if (processInstanceContexts != null) {
                removeInactiveTasks(processInstanceContexts);
                LOGGER.debug("processes: {}", processInstanceContexts.size());
                processInstanceContexts.forEach(this::scheduleRequest);
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred: {}", e.getMessage(), e);
        }
    }

    private void removeInactiveTasks(final List<ProcessInstanceContext> processInstanceContexts) {
        List<String> activeTasks =
                processInstanceContexts.stream().map(this::getRequestTaskKey).toList();

        List<Map.Entry<String, ScheduledFuture<?>>> inactiveTasks =
                runningGraphqlRequestTaskIds.entrySet().stream()
                        .filter(entry -> !activeTasks.contains(entry.getKey()))
                        .toList();

        inactiveTasks.forEach(
                entry -> {
                    entry.getValue().cancel(true);
                    runningGraphqlRequestTaskIds.remove(entry.getKey());
                });
    }

    private void scheduleRequest(ProcessInstanceContext processInstanceContext) {
        String taskKey = getRequestTaskKey(processInstanceContext);
        LOGGER.debug("Scheduling request for {}", taskKey);
        runningGraphqlRequestTaskIds.computeIfAbsent(
                taskKey,
                (key) -> {
                    LOGGER.debug("Creating new task for context: {}", processInstanceContext);
                    var task = new GraphQlRequestTask(processInstanceContext);
                    return this.executorService
                            .getExecutorService()
                            .schedule(task, 0, TimeUnit.MILLISECONDS);
                });
    }

    private String getRequestTaskKey(final ProcessInstanceContext processInstanceContext) {
        return context.getDefinition().elementId() + processInstanceContext.getKey();
    }

    public void start() {
        LOGGER.debug("{}.start()", getClass().getSimpleName());
        executorService
                .getExecutorService()
                .scheduleWithFixedDelay(
                        this, 0, config.getOperatePollingInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        LOGGER.debug("{}.stop()", getClass().getSimpleName());
        runningGraphqlRequestTaskIds.values().forEach(task -> task.cancel(true));
    }
}
