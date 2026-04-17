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

    public SsfTransmitterResource(KeycloakSession session, AuthenticationManager.AuthResult authResult) {
        this.session = session;
        this.authResult = authResult;
        this.transmitter = session.getProvider(SsfTransmitterProvider.class);
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

    @Path("/subjects/add")
    public SsfSubjectManagementResource getAddSubjectEndpoint() {
        if (!transmitter.getConfig().isSubjectManagementEnabled()) {
            throw new NotFoundException();
        }
        return transmitter.subjectManagementResource();
    }

    @Path("/subjects/remove")
    public SsfSubjectManagementResource getRemoveSubjectEndpoint() {
        if (!transmitter.getConfig().isSubjectManagementEnabled()) {
            throw new NotFoundException();
        }
        return transmitter.subjectManagementResource();
    }
}
