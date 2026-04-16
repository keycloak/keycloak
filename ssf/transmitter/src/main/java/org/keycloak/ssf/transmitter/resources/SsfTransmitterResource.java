package org.keycloak.ssf.transmitter.resources;

import jakarta.ws.rs.Path;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.ssf.transmitter.SsfTransmitter;

public class SsfTransmitterResource {

    protected final KeycloakSession session;

    private final AuthenticationManager.AuthResult authResult;

    public SsfTransmitterResource(KeycloakSession session, AuthenticationManager.AuthResult authResult) {
        this.session = session;
        this.authResult = authResult;
    }

    @Path("/streams")
    public SsfStreamManagementResource getStreamManagementEndpoint() {
        return SsfTransmitter.current().streamManagementResource();
    }

    @Path("/streams/status")
    public SsfStreamStatusResource getStreamStatusEndpoint() {
        return SsfTransmitter.current().streamStatusResource();
    }

    @Path("/verify")
    public SsfStreamVerificationResource getVerificationEndpoint() {
        return SsfTransmitter.current().streamVerificationResource();
    }
}
