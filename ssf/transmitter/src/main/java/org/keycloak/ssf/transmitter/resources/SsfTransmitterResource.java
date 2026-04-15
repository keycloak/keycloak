package org.keycloak.ssf.transmitter.resources;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.transmitter.SsfTransmitter;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.utils.KeycloakSessionUtil;

public class SsfTransmitterResource {

    protected final KeycloakSession session;

    public SsfTransmitterResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("/streams")
    public SsfStreamManagementResource getStreamManagementEndpoint() {
        authenticate();
        return SsfTransmitter.current().streamManagementResource();
    }

    @Path("/streams/status")
    public SsfStreamStatusResource getStreamStatusEndpoint() {
        authenticate();
        return SsfTransmitter.current().streamStatusResource();
    }

    @Path("/verify")
    public SsfStreamVerificationResource getVerificationEndpoint() {
        authenticate();
        return SsfTransmitter.current().streamVerificationResource();
    }

    protected AuthenticationManager.AuthResult authenticate() {
        var authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        var auth = authenticator.authenticate();
        if (auth == null) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        // make the current authentication available on the session
        SsfAuthUtil.setAuth(session, auth);
        return auth;
    }
}
