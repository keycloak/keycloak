package org.keycloak.ssf.transmitter.resources;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;

public class SsfTransmitterResource {

    protected final KeycloakSession session;

    protected final SsfTransmitterProvider transmitter;

    private final AuthenticationManager.AuthResult authResult;

    public SsfTransmitterResource(KeycloakSession session, AuthenticationManager.AuthResult authResult, SsfTransmitterProvider transmitter) {
        this.session = session;
        this.authResult = authResult;
        this.transmitter = transmitter;
    }

    @Path("/streams")
    public SsfStreamManagementResource getStreamManagementEndpoint() {
        return transmitter.streamManagementResource();
    }

    @Path("/streams/status")
    public SsfStreamStatusResource getStreamStatusEndpoint() {
        return transmitter.streamStatusResource();
    }

    @Path("/verify")
    public SsfStreamVerificationResource getVerificationEndpoint() {
        return transmitter.streamVerificationResource();
    }

    /**
     * Single sub-resource locator covering both
     * {@code /subjects/add} and {@code /subjects/remove}. The two
     * actions are disambiguated by per-method {@code @Path("add")} /
     * {@code @Path("remove")} on
     * {@link SsfSubjectManagementResource}. Splitting the locator
     * (returning the same sub-resource from {@code /subjects/add}
     * AND {@code /subjects/remove}) used to confuse JAX-RS — both
     * paths would route to whichever {@code @POST} method got picked
     * first.
     */
    @Path("/subjects")
    public SsfSubjectManagementResource getSubjectManagementEndpoint() {
        if (!transmitter.getConfig().isSubjectManagementEnabled()) {
            throw new NotFoundException();
        }
        return transmitter.subjectManagementResource();
    }

    /**
     * RFC 8936 polling endpoint per SSF §6.1.2. Sub-resource locator —
     * the stream-id and client-id path parameters are consumed by
     * {@link SsfStreamPollResource#poll}. The receiver-facing URL shape
     * is {@code /ssf/transmitter/receivers/{clientId}/streams/{streamId}/poll},
     * matching the {@code delivery.endpoint_url} the transmitter writes
     * back into the stream-create response.
     */
    @Path("/receivers/{clientId}/streams/{streamId}/poll")
    public SsfStreamPollResource getStreamPollEndpoint() {
        return new SsfStreamPollResource(session, transmitter.streamStore(), transmitter.pollDeliveryService());
    }
}
