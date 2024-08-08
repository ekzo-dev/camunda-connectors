package dev.ekzo.camunda.connector.inbound.graphql.model;

import lombok.Data;

import java.util.List;

/**
 * @author sergey.syroezhkin
 * @since 13.07.2024
 */
@Data
public class Request {

    public static final String STATE_FAIL = "fail";
    public static final String STATE_ERROR = "error";
    public static final String STATE_SUCCESS = "success";

    private Integer id;
    private String state;
    private Object result;
    private List<Object> responseAttachments;
}
