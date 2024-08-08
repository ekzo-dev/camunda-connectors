package dev.ekzo.camunda.connector.inbound.graphql.model;

import io.camunda.connector.feel.annotation.FEEL;
import lombok.Data;

/**
 * @author sergey.syroezhkin
 * @since 23.02.2024
 */
@Data
public class EkzoGraphQLRequest {

    @FEEL private String endpointSlug;
    @FEEL private Object params;
    private String notify;
    private String query;

}
