package dev.ekzo.camunda.connector.inbound.graphql.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.hosopy.actioncable.ActionCable;
import com.hosopy.actioncable.Channel;
import com.hosopy.actioncable.Consumer;
import com.hosopy.actioncable.Subscription;
import dev.ekzo.camunda.connector.inbound.graphql.model.EkzoConnectParams;
import dev.ekzo.camunda.connector.inbound.graphql.model.EkzoGraphQLRequestWrapper;
import dev.ekzo.camunda.connector.inbound.graphql.model.EkzoGraphQLResponseBody;
import dev.ekzo.camunda.connector.inbound.graphql.model.EkzoGraphQLSubscribeResponseWrapper;
import dev.ekzo.camunda.connector.inbound.graphql.model.Request;
import dev.ekzo.camunda.connector.inbound.graphql.model.RequestStateChangedResponse;
import io.camunda.connector.api.inbound.ProcessInstanceContext;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.http.base.HttpService;
import io.camunda.connector.http.base.model.HttpCommonRequest;
import io.camunda.connector.http.base.model.HttpCommonResult;
import io.camunda.connector.http.base.model.HttpMethod;
import io.camunda.connector.http.base.model.auth.NoAuthentication;
import io.camunda.connector.http.graphql.model.GraphQLRequest;
import io.camunda.connector.http.graphql.utils.GraphQLRequestMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class GraphQlRequestTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQlRequestTask.class);

    private static final String ENV_GRAPHQL_ENDPOINT = "EKZO_CONNECT_GRAPHQL_ENDPOINT";
    private static final String ENV_GRAPHQL_WEBSOCKET_ENDPOINT = "EKZO_CONNECT_GRAPHQL_WEBSOCKET_ENDPOINT";
    private static final TypeReference<EkzoGraphQLSubscribeResponseWrapper<RequestStateChangedResponse>> REQUEST_STATE_CHANGED_TYPE = new TypeReference<>() { };
    private static final String QUERY_TEMPLATE = "subscription RequestFinishedSubscription($id: Int!) {\n" +
            "    requestStateChanged(id: $id) {\n" +
            "        request {\n" +
            "            id\n" +
            "            state\n" +
            "            result\n" +
            "            responseAttachments {\n" +
            "                url\n" +
            "                filename\n" +
            "                byteSize\n" +
            "                contentType\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private final ProcessInstanceContext processInstanceContext;

    private final ObjectMapper objectMapper;
    private final HttpService httpService;
    private final GraphQLRequestMapper graphQLRequestMapper;


    public GraphQlRequestTask(ProcessInstanceContext processInstanceContext,
                              ObjectMapper objectMapper) {
        LOGGER.debug("new {}()", getClass().getSimpleName());
        this.processInstanceContext = processInstanceContext;
        this.objectMapper = objectMapper;
        this.httpService = new HttpService();
        this.graphQLRequestMapper = new GraphQLRequestMapper(objectMapper);
    }

    public GraphQlRequestTask(ProcessInstanceContext processInstanceContext) {
        this(processInstanceContext, ConnectorsObjectMapperSupplier.getCopy());
    }

    @Override
    public void run() {
        LOGGER.debug("Running GraphQlRequestTask");
        try {
            var wrapper = processInstanceContext.bind(EkzoGraphQLRequestWrapper.class);
            if (wrapper != null) {
                try {
                    LOGGER.debug("Preparing GraphQL request");

                    GraphQLRequest graphQLRequest = prepareRequest(wrapper);
                    var result = executeGraphQLConnector(graphQLRequest);
                    LOGGER.info("GRAPHQL RESULT: {}", result);

                    Object aCorrelationId = ((Map) wrapper.getGraphql().getParams()).get("aCorrelationId");
                    LOGGER.info("Got aCorrelationId param: {}", aCorrelationId);

                    Request requestResult = extractRequestResult(result.body());
                    if (requestResult != null) {
                        if (EkzoGraphQLResponseBody.QUEUED.equals(requestResult.getState())) {

                            subscribeForResult(graphQLRequest, requestResult.getId(), aCorrelationId, processInstanceContext);

                        } else {

                            LOGGER.info("Status is not queued - returning result");

                            processGraphqlResult(result, aCorrelationId, processInstanceContext);
                        }
                    } else {
                        LOGGER.warn(
                                "Empty or null request found in Graphql response for process instance {}",
                                processInstanceContext);

                    }
                } catch (Exception e) {
                    LOGGER.warn(
                            "Exception encountered while executing HTTP request for process instance {}: {}",
                            processInstanceContext,
                            e.getMessage());
                }

            } else {
                LOGGER.debug(
                        "No HTTP request binding found for process instance {}",
                        processInstanceContext);
            }
        } catch (Exception e) {
            LOGGER.warn(
                    "Error occurred while binding properties for processInstanceKey {}: {}",
                    processInstanceContext.getKey(),
                    e.getMessage());
        }
    }

    @SneakyThrows({URISyntaxException.class})
    private void subscribeForResult(GraphQLRequest graphQLRequest, Integer id, Object aCorrelationId, ProcessInstanceContext processInstanceContext) {
        LOGGER.info("Subscribing for result for request id {}", id);
        
        URI uri = new URI(System.getenv(ENV_GRAPHQL_WEBSOCKET_ENDPOINT));
        Consumer.Options options = new Consumer.Options();
        options.reconnection = true;
//    options.pingInterval = 30l;
//    options.pingTimeUnit = TimeUnit.SECONDS;
        Consumer consumer = ActionCable.createConsumer(uri, options);

        Channel appearanceChannel = new Channel("GraphqlChannel");
        Subscription subscription = consumer.getSubscriptions().create(appearanceChannel);

        subscription
                .onConnected(() -> {
                    LOGGER.info("onConnected");

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("query", QUERY_TEMPLATE);
                    jsonObject.addProperty("operationName", "RequestFinishedSubscription");
                    JsonObject variables = new JsonObject();
                    variables.addProperty("id", id);
                    jsonObject.add("variables", variables);
                    subscription.perform("execute", jsonObject);

                })
                .onRejected(() -> {
                    LOGGER.info("onRejected");
                })
                .onReceived(data -> {
                    LOGGER.info("onReceived: {}", data);
                    boolean needContinue = true;
                    try {
                        needContinue = processSubscriptionData(data, aCorrelationId, processInstanceContext);
                    } catch (Exception e) {
                        LOGGER.error("An error was caught on processing subscription data", e);
                        needContinue = false;
                    } finally {
                        if (!needContinue) {
                            LOGGER.info("Stopping listener");
                            consumer.disconnect();
                        }
                    }
                })
                .onDisconnected(() -> {
                    LOGGER.info("onDisconnected");
                })
                .onFailed(e -> {
                    LOGGER.error("onFailed", e);
                });

        consumer.connect();

    }

    private boolean processSubscriptionData(Object data, Object aCorrelationId, ProcessInstanceContext processInstanceContext) {
        EkzoGraphQLSubscribeResponseWrapper<RequestStateChangedResponse> response = null;
        if (data instanceof JsonObject) {
            try {
                response = objectMapper.readValue(data.toString(), REQUEST_STATE_CHANGED_TYPE);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot deserialize graphql subscribe message", e);
            }
        } else {
            try {
                response = objectMapper.convertValue(data, REQUEST_STATE_CHANGED_TYPE);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot deserialize graphql subscribe message", e);
            }
        }
        Optional<Request> request = Optional.ofNullable(response)
                .map(EkzoGraphQLSubscribeResponseWrapper::getResult)
                .map(EkzoGraphQLSubscribeResponseWrapper.Result::getData)
                .map(RequestStateChangedResponse::getRequestStateChanged)
                .map(RequestStateChangedResponse.RequestStateChanged::getRequest);
        String state = request.map(Request::getState).orElse("UNDEFINED");
        switch (state) {
            case Request.STATE_SUCCESS, Request.STATE_ERROR, Request.STATE_FAIL -> {
                LOGGER.info("Processing subscribe response with state {}", state);
                processGraphqlResult(request.map(Request::getResult).orElse(null), aCorrelationId, processInstanceContext);
                return false;
            }
            default -> {
                LOGGER.debug("Ignoring state '{}'", state);
                return true;
            }
        }


    }
    private void processGraphqlResult(Object result, Object aCorrelationId, ProcessInstanceContext processInstanceContext) {
        var wrappedResult = Map.of(
                "result", result,
                "aCorrelationId", aCorrelationId);

        LOGGER.info("Correlating variables: {}", wrappedResult);
        processInstanceContext.correlate(wrappedResult);
    }

    private Request extractRequestResult(Object body) {
        if (body != null) {
            try {
                EkzoGraphQLResponseBody bodyVar = objectMapper.convertValue(body, EkzoGraphQLResponseBody.class);
                return Optional.ofNullable(bodyVar)
                        .map(EkzoGraphQLResponseBody::getData)
                        .map(EkzoGraphQLResponseBody.ResponseData::getCreateRequest)
                        .map(EkzoGraphQLResponseBody.CreateRequestResult::getRequest)
                        .orElse(null);
            } catch (Exception e) {
                LOGGER.warn("Cannot convert request result from object: {}", body, e);
            }
        } else {
            LOGGER.warn("Graphql response body is empty or null");
        }
        return null;
    }

    private GraphQLRequest prepareRequest(EkzoGraphQLRequestWrapper wrapper) {
        EkzoConnectParams variables = EkzoConnectParams.of(
                wrapper.getGraphql().getEndpointSlug(),
                prepareParams(wrapper.getGraphql().getParams()),
                null);
//              UUID.randomUUID().toString());
        return new GraphQLRequest(
                new GraphQLRequest.GraphQL(
                        wrapper.getGraphql().getQuery(),
                        objectMapper.convertValue(variables, Map.class),
                        HttpMethod.POST,
                        System.getenv(ENV_GRAPHQL_ENDPOINT),
                        Collections.emptyMap(),
                        5
                ),
                new NoAuthentication());
    }

    private Object prepareParams(Object params) {
        if (params instanceof String) {
            if (((String) params).startsWith("=")) {
                try {
                    return objectMapper.readValue(((String) params).substring(1), Map.class);
                } catch (Exception e) {
                    LOGGER.warn("Cannot deserialize params", e);
                }
            }
        }
        return params;
    }

    private HttpCommonResult executeGraphQLConnector(final GraphQLRequest graphQLRequest) {
        // connector logic
        LOGGER.debug("Executing graphql connector with request {}", graphQLRequest);

        HttpCommonRequest commonRequest = graphQLRequestMapper.toHttpCommonRequest(graphQLRequest);
        HttpCommonResult result = httpService.executeConnectorRequest(commonRequest);

        LOGGER.debug("Graphql result {}", result);
        return result;
    }

}
