package org.keycloak.ssf.transmitter;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.support.SsfAuthUtil;
import org.keycloak.ssf.transmitter.resources.StreamManagementResource;
import org.keycloak.ssf.transmitter.resources.StreamStatusResource;
import org.keycloak.ssf.transmitter.resources.StreamVerificationResource;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.utils.KeycloakSessionUtil;

public class SsfTransmitterResource {

    @Path("/streams")
    public StreamManagementResource getStreamManagementEndpoint() {
        authenticate();
        return Ssf.transmitter().streamManagementEndpoint();
    }

    @Path("/streams/status")
    public StreamStatusResource getStreamStatusEndpoint() {
        authenticate();
        return Ssf.transmitter().streamStatusEndpoint();
    }

    @Path("/verify")
    public StreamVerificationResource getVerificationEndpoint() {
        authenticate();
        return Ssf.transmitter().verificationEndpoint();
    }

    protected AuthenticationManager.AuthResult authenticate() {
        var session = KeycloakSessionUtil.getKeycloakSession();
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
