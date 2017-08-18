package org.keycloak.protocol.saml.profile.ecp.authenticator;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.Base64;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

public class HttpBasicAuthenticator implements Authenticator {

    private static final String BASIC = "Basic";
    private static final String BASIC_PREFIX = BASIC + " ";

    @Override
    public void authenticate(final AuthenticationFlowContext context) {
        final HttpRequest httpRequest = context.getHttpRequest();
        final HttpHeaders httpHeaders = httpRequest.getHttpHeaders();
        final String[] usernameAndPassword = getUsernameAndPassword(httpHeaders);

        context.attempted();

        if (usernameAndPassword != null) {
            final RealmModel realm = context.getRealm();
            final String username = usernameAndPassword[0];
            final UserModel user = context.getSession().users().getUserByUsername(username, realm);

            if (user != null) {
                final String password = usernameAndPassword[1];
                final boolean valid = context.getSession().userCredentialManager().isValid(realm, user, UserCredentialModel.password(password));

                if (valid) {
                    if (user.isEnabled()) {
                        userSuccessAction(context, user);
                    } else {
                        userDisabledAction(context, realm, user);
                    }
                } else {
                    notValidCredentialsAction(context, realm, user);
                }
            } else {
                nullUserAction(context, realm, username);
            }
        }
    }

    protected void userSuccessAction(AuthenticationFlowContext context, UserModel user) {
        context.getAuthenticationSession().setAuthenticatedUser(user);
        context.success();
    }

    protected void userDisabledAction(AuthenticationFlowContext context, RealmModel realm, UserModel user) {
        userSuccessAction(context, user);
    }

    protected void nullUserAction(final AuthenticationFlowContext context, final RealmModel realm, final String user) {
        // no-op by default
    }

    protected void notValidCredentialsAction(final AuthenticationFlowContext context, final RealmModel realm, final UserModel user) {
        context.getEvent().user(user);
        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
        context.failure(AuthenticationFlowError.INVALID_USER, Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_PREFIX + "realm=\"" + realm.getName() + "\"")
                .build());
    }

    private String[] getUsernameAndPassword(final HttpHeaders httpHeaders) {
        final List<String> authHeaders = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);

        if (authHeaders == null || authHeaders.size() == 0) {
            return null;
        }

        String credentials = null;

        for (final String authHeader : authHeaders) {
            if (authHeader.startsWith(BASIC_PREFIX)) {
                final String[] split = authHeader.trim().split("\\s+");

                if (split == null || split.length != 2) return null;

                credentials = split[1];
            }
        }

        try {
            return new String(Base64.decode(credentials)).split(":");
        } catch (final IOException e) {
            throw new RuntimeException("Failed to parse credentials.", e);
        }
    }

    @Override
    public void action(final AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(final KeycloakSession session, final RealmModel realm, final UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(final KeycloakSession session, final RealmModel realm, final UserModel user) {

    }

    @Override
    public void close() {

    }
}
