/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package dev.ekzo.camunda.connector.inbound.graphql;

import dev.ekzo.camunda.connector.inbound.graphql.service.SharedExecutorService;
import dev.ekzo.camunda.connector.inbound.graphql.task.ProcessInstancesFetcherTask;
import io.camunda.connector.api.annotation.InboundConnector;
import io.camunda.connector.api.inbound.InboundConnectorExecutable;
import io.camunda.connector.api.inbound.InboundIntermediateConnectorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InboundConnector(name = "EKZO_INBOUND_GRAPHQL", type = "dev.ekzo.camunda:graphql-inbound:1")
public class EkzoInboundGraphqlConnector
    implements InboundConnectorExecutable<InboundIntermediateConnectorContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(EkzoInboundGraphqlConnector.class);

  private final SharedExecutorService executorService;

  private ProcessInstancesFetcherTask processInstancesFetcherTask;

  public EkzoInboundGraphqlConnector() {
    this(SharedExecutorService.getInstance());}

  public EkzoInboundGraphqlConnector(
      final SharedExecutorService executorService) {
    LOGGER.debug("{} init", getClass().getSimpleName());
    this.executorService = executorService;
  }

  @Override
  public void activate(final InboundIntermediateConnectorContext context) {
    LOGGER.debug("{} activate", getClass().getSimpleName());
    processInstancesFetcherTask = new ProcessInstancesFetcherTask(context, executorService);
    processInstancesFetcherTask.start();
  }

  @Override
  public void deactivate() {
    LOGGER.debug("Deactivating the Ekzo Inbound GraphQL Connector");
    processInstancesFetcherTask.stop();
  }
}
