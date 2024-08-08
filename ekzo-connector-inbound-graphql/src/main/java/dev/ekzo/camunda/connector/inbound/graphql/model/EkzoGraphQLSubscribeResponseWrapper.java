package dev.ekzo.camunda.connector.inbound.graphql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sergey.syroezhkin
 * @since 13.07.2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EkzoGraphQLSubscribeResponseWrapper<T> {

    private Result<T> result;
    private Boolean more;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result<K> {
        private K data;
    }

}
