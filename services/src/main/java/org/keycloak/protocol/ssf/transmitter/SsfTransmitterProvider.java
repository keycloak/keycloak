package org.keycloak.protocol.ssf.transmitter;

import org.keycloak.protocol.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.protocol.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadataService;
import org.keycloak.protocol.ssf.transmitter.resources.StreamManagementResource;
import org.keycloak.protocol.ssf.transmitter.resources.StreamStatusResource;
import org.keycloak.protocol.ssf.transmitter.resources.StreamVerificationResource;
import org.keycloak.protocol.ssf.transmitter.stream.StreamService;
import org.keycloak.protocol.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.provider.Provider;

/**
 * Provider for the SSF (Shared Signals Framework) Transmitter.
 *
 * <p>The transmitter is responsible for generating and delivering Security Event Tokens (SETs)
 * to registered SSF receivers via configured streams. It exposes services for stream management,
 * stream verification, event mapping, and event dispatching, as well as the JAX-RS sub-resources
 * that implement the SSF Transmitter REST API.
 *
 * @see <a href="https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html">OpenID Shared Signals Framework 1.0</a>
 */
public interface SsfTransmitterProvider extends Provider {

    default void close() {
    }

    /**
     * Returns the service for handling stream verification requests.
     *
     * @return the stream verification service
     */
    StreamVerificationService verificationService();

    /**
     * Returns the service for managing transmitter metadata,
     * such as supported events, delivery methods, and the transmitter configuration endpoint.
     *
     * @return the transmitter metadata service
     */
    SsfTransmitterMetadataService transmitterService();

    /**
     * Returns the mapper that converts Keycloak events (user events, admin events)
     * into SSF Security Event Tokens (SETs).
     *
     * @return the security event token mapper
     */
    SecurityEventTokenMapper securityEventTokenMapper();

    /**
     * Returns the dispatcher responsible for delivering Security Event Tokens
     * to all applicable streams based on their delivery configuration.
     *
     * @return the security event token dispatcher
     */
    SecurityEventTokenDispatcher securityEventTokenDispatcher();

    /**
     * Returns the JAX-RS sub-resource for stream CRUD operations (create, read, update, delete).
     *
     * @return the stream management endpoint
     */
    StreamManagementResource streamManagementEndpoint();

    /**
     * Returns the service for managing SSF streams (create, update, delete, lookup).
     *
     * @return the stream service
     */
    StreamService streamService();

    /**
     * Returns the JAX-RS sub-resource for querying and updating stream status.
     *
     * @return the stream status endpoint
     */
    StreamStatusResource streamStatusEndpoint();

    /**
     * Returns the JAX-RS sub-resource for triggering stream verification.
     *
     * @return the stream verification endpoint
     */
    StreamVerificationResource verificationEndpoint();
}
