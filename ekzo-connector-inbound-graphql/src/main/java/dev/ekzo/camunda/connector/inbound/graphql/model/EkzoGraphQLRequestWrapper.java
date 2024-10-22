
package dev.ekzo.camunda.connector.inbound.graphql.model;

import io.camunda.connector.feel.annotation.FEEL;
import io.camunda.connector.http.base.model.auth.Authentication;
import lombok.Data;

@Data
public class EkzoGraphQLRequestWrapper {

    private Authentication authentication;
    private EkzoGraphQLRequest graphql;
    @FEEL private String correlationKey;

}
