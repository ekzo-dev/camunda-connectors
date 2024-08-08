package dev.ekzo.camunda.connector.inbound.graphql.model;

import lombok.Data;

/**
 * @author sergey.syroezhkin
 * @since 13.07.2024
 */
@Data
public class RequestStateChangedResponse {

    private RequestStateChanged requestStateChanged;

    @Data
    public static class RequestStateChanged {
        private Request request;
    }
}
