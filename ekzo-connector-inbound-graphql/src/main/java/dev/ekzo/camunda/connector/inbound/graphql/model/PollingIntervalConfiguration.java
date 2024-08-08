package dev.ekzo.camunda.connector.inbound.graphql.model;

import io.camunda.connector.feel.annotation.FEEL;
import lombok.Data;

import java.time.Duration;

@Data
public class PollingIntervalConfiguration {

    private static final Duration DEFAULT_OPERATE_INTERVAL = Duration.ofSeconds(5);

    @FEEL
    private Duration operatePollingInterval = DEFAULT_OPERATE_INTERVAL;

}
