package org.keycloak.federation.scim.core.service;

import com.google.common.net.HttpHeaders;
import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.ScimRequestBuilder;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.ws.rs.ProcessingException;
import org.jboss.logging.Logger;
import org.keycloak.federation.scim.core.ScrimEndPointConfiguration;
import org.keycloak.federation.scim.core.exceptions.InvalidResponseFromScimEndpointException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScimClient<S extends ResourceNode> implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ScimClient.class);

    private final RetryRegistry retryRegistry;

    private final ScimRequestBuilder scimRequestBuilder;

    private final ScimResourceType scimResourceType;
    private final boolean logAllRequests;

    private ScimClient(ScimRequestBuilder scimRequestBuilder, ScimResourceType scimResourceType, boolean detailedLogs) {
        this.scimRequestBuilder = scimRequestBuilder;
        this.scimResourceType = scimResourceType;
        RetryConfig retryConfig = RetryConfig.custom().maxAttempts(10).intervalFunction(IntervalFunction.ofExponentialBackoff())
                .retryExceptions(ProcessingException.class).build();
        retryRegistry = RetryRegistry.of(retryConfig);
        this.logAllRequests = detailedLogs;
    }

    public static <T extends ResourceNode> ScimClient<T> open(ScrimEndPointConfiguration scimProviderConfiguration,
            ScimResourceType scimResourceType) {
        String scimApplicationBaseUrl = scimProviderConfiguration.getEndPoint();
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put(HttpHeaders.AUTHORIZATION, scimProviderConfiguration.getAuthorizationHeaderValue());
        httpHeaders.put(HttpHeaders.CONTENT_TYPE, scimProviderConfiguration.getContentType());
        ScimClientConfig scimClientConfig = ScimClientConfig.builder().httpHeaders(httpHeaders).connectTimeout(5)
                .requestTimeout(5).socketTimeout(5).build();
        ScimRequestBuilder scimRequestBuilder = new ScimRequestBuilder(scimApplicationBaseUrl, scimClientConfig);
        return new ScimClient<>(scimRequestBuilder, scimResourceType, scimProviderConfiguration.isLogAllScimRequests());
    }

    public String create(String id, S scimForCreation) throws InvalidResponseFromScimEndpointException {
        Optional<String> scimForCreationId = scimForCreation.getId();
        if (scimForCreationId.isPresent()) {
            throw new IllegalArgumentException(
                    "User to create should never have an existing id: %s %s".formatted(id, scimForCreationId.get()));
        }
        try {
            Retry retry = retryRegistry.retry("create-%s".formatted(id));
            if (logAllRequests) {
                LOGGER.info("[SCIM] Sending CREATE " + scimForCreation.toPrettyString() + "\n to " + getScimEndpoint());
            }
            ServerResponse<S> response = retry.executeSupplier(() -> scimRequestBuilder
                    .create(getResourceClass(), getScimEndpoint()).setResource(scimForCreation).sendRequest());
            checkResponseIsSuccess(response);
            S resource = response.getResource();
            return resource.getId().orElseThrow(
                    () -> new InvalidResponseFromScimEndpointException(response, "Created SCIM resource does not have id"));

        } catch (Exception e) {
            LOGGER.warn(e);
            throw new InvalidResponseFromScimEndpointException("Exception while retrying create " + e.getMessage(), e);
        }
    }

    private void checkResponseIsSuccess(ServerResponse<S> response) throws InvalidResponseFromScimEndpointException {
        if (logAllRequests) {
            LOGGER.info("[SCIM] Server response " + response.getHttpStatus() + "\n" + response.getResponseBody());
        }
        if (!response.isSuccess()) {
            throw new InvalidResponseFromScimEndpointException(response,
                    "Server answered with status " + response.getResponseBody() + ": " + response.getResponseBody());
        }
    }

    private String getScimEndpoint() {
        return scimResourceType.getEndpoint();
    }

    private Class<S> getResourceClass() {
        return scimResourceType.getResourceClass();
    }

    public void update(String externalId, S scimForReplace) throws InvalidResponseFromScimEndpointException {
        Retry retry = retryRegistry.retry("replace-%s".formatted(externalId));
        try {
            if (logAllRequests) {
                LOGGER.info("[SCIM] Sending UPDATE " + scimForReplace.toPrettyString() + "\n to " + getScimEndpoint());
            }
            ServerResponse<S> response = retry.executeSupplier(
                    () -> scimRequestBuilder.update(getResourceClass(), getScimEndpoint(), externalId)
                            .setResource(scimForReplace).sendRequest());
            checkResponseIsSuccess(response);
        } catch (Exception e) {
            LOGGER.warn(e);
            throw new InvalidResponseFromScimEndpointException("Exception while retrying update " + e.getMessage(), e);
        }
    }

    public void delete(String externalId) throws InvalidResponseFromScimEndpointException {
        Retry retry = retryRegistry.retry("delete-%s".formatted(externalId));
        if (logAllRequests) {
            LOGGER.info("[SCIM] Sending DELETE to " + getScimEndpoint());
        }
        try {
            ServerResponse<S> response = retry.executeSupplier(() -> scimRequestBuilder
                    .delete(getResourceClass(), getScimEndpoint(), externalId).sendRequest());
            checkResponseIsSuccess(response);
        } catch (Exception e) {
            LOGGER.warn(e);
            throw new InvalidResponseFromScimEndpointException("Exception while retrying delete " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        scimRequestBuilder.close();
    }

    public List<S> listResources() {
        ServerResponse<ListResponse<S>> response = scimRequestBuilder.list(getResourceClass(), getScimEndpoint()).get()
                .sendRequest();
        ListResponse<S> resourceTypeListResponse = response.getResource();
        return resourceTypeListResponse.getListedResources();
    }
}
