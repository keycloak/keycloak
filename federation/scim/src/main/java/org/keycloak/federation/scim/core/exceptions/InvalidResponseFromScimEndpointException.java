package org.keycloak.federation.scim.core.exceptions;

import de.captaingoldfish.scim.sdk.client.response.ServerResponse;

import java.util.Optional;

public class InvalidResponseFromScimEndpointException extends ScimPropagationException {

    private final transient Optional<ServerResponse> response;

    public InvalidResponseFromScimEndpointException(ServerResponse response, String message) {
        super(message);
        this.response = Optional.of(response);
    }

    public InvalidResponseFromScimEndpointException(String message, Exception e) {
        super(message, e);
        this.response = Optional.empty();
    }

    /**
     * Empty response can occur if a major exception was thrown while retrying the request.
     */
    public Optional<ServerResponse> getResponse() {
        return response;
    }

}
