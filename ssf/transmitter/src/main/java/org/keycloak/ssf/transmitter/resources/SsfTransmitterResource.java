package org.keycloak.ssf.transmitter.resources;

import jakarta.ws.rs.Path;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;

public class SsfTransmitterResource {

    protected final KeycloakSession session;

    private final AuthenticationManager.AuthResult authResult;

    public SsfTransmitterResource(KeycloakSession session, AuthenticationManager.AuthResult authResult) {
        this.session = session;
        this.authResult = authResult;
    }

    @Path("/streams")
    public SsfStreamManagementResource getStreamManagementEndpoint() {
        return session.getProvider(SsfTransmitterProvider.class).streamManagementResource();
    }

    @Path("/streams/status")
    public SsfStreamStatusResource getStreamStatusEndpoint() {
        return session.getProvider(SsfTransmitterProvider.class).streamStatusResource();
    }

    @Path("/verify")
    public SsfStreamVerificationResource getVerificationEndpoint() {
        return session.getProvider(SsfTransmitterProvider.class).streamVerificationResource();
    }
}
