package dev.ekzo.camunda.connector.inbound.graphql.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
public class EkzoGraphQLResponseBody {

    public static final String QUEUED = "queued";


    private ResponseData data;

    @Data
    @ToString
    @EqualsAndHashCode
    public static class ResponseData {
        private CreateRequestResult createRequest;
    }

    @Data
    @ToString
    @EqualsAndHashCode
    public static class CreateRequestResult {
        private Request request;
    }

}
