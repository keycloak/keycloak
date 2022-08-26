package org.keycloak.protocol.saml.profile.ecp.authenticator;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.Base64;
import org.keycloak.events.Details;
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
            final UserModel user = context.getSession().users().getUserByUsername(realm, username);

            // to allow success/failure logging for brute force
            context.getEvent().detail(Details.USERNAME, username);
            context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

            if (user != null) {
                final String password = usernameAndPassword[1];
                final boolean valid = user.credentialManager().isValid(UserCredentialModel.password(password));

                if (valid) {
                    if (isTemporarilyDisabledByBruteForce(context, user)) {
                        userDisabledAction(context, realm, user, Errors.USER_TEMPORARILY_DISABLED);
                    } else if (user.isEnabled()) {
                        userSuccessAction(context, user);
                    } else {
                        userDisabledAction(context, realm, user, Errors.USER_DISABLED);
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

    protected void userDisabledAction(AuthenticationFlowContext context, RealmModel realm, UserModel user, String eventError) {
        context.getEvent().user(user);
        context.getEvent().error(eventError);
        context.failure(AuthenticationFlowError.INVALID_USER, Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_PREFIX + "realm=\"" + realm.getName() + "\"")
                .build());
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

    private boolean isTemporarilyDisabledByBruteForce(AuthenticationFlowContext context, UserModel user) {
        return (context.getRealm().isBruteForceProtected())
           && (context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user));
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

                if (split.length != 2) return null;

                credentials = split[1];
            }
        }

        try {
            String val = new String(Base64.decode(credentials));
            int seperatorIndex = val.indexOf(":");
            if(seperatorIndex == -1) return new String[]{val};
            String user = val.substring(0, seperatorIndex);
            String pw = val.substring(seperatorIndex + 1);
            return new String[]{user,pw};
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
