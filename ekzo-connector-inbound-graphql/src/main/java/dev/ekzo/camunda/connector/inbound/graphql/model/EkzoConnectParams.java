package dev.ekzo.camunda.connector.inbound.graphql.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Optional;

/**
 * @author sergey.syroezhkin
 * @since 23.02.2024
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EkzoConnectParams implements Serializable {
    
    private Input input;

    public static EkzoConnectParams of(String endpointSlug, Object params, String notify) {
        return new EkzoConnectParams(new Input(endpointSlug, Optional.ofNullable(params).orElseGet(HashMap::new), notify));

    }

    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Input implements Serializable {
        private String endpointSlug;
        private Object params;
        private String notify;
    }
}
