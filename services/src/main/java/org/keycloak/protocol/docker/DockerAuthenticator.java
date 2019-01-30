package org.keycloak.protocol.docker;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.saml.profile.ecp.authenticator.HttpBasicAuthenticator;
import org.keycloak.representations.docker.DockerAccess;
import org.keycloak.representations.docker.DockerError;
import org.keycloak.representations.docker.DockerErrorResponseToken;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

public class DockerAuthenticator extends HttpBasicAuthenticator {
    private static final Logger logger = Logger.getLogger(DockerAuthenticator.class);

    public static final String ID = "docker-http-basic-authenticator";

    @Override
    protected void notValidCredentialsAction(final AuthenticationFlowContext context, final RealmModel realm, final UserModel user) {
        invalidUserAction(context, realm, user.getUsername(), context.getSession().getContext().resolveLocale(user));
    }

    @Override
    protected void nullUserAction(final AuthenticationFlowContext context, final RealmModel realm, final String userId) {
        final String localeString = Optional.ofNullable(realm.getDefaultLocale()).orElse(Locale.ENGLISH.toString());
        invalidUserAction(context, realm, userId, new Locale(localeString));
    }

    @Override
    protected void userDisabledAction(AuthenticationFlowContext context, RealmModel realm, UserModel user, String eventError) {
        context.getEvent().user(user);
        context.getEvent().error(eventError);
        final DockerError error = new DockerError("UNAUTHORIZED","Invalid username or password.",
                Collections.singletonList(new DockerAccess(context.getAuthenticationSession().getClientNote(DockerAuthV2Protocol.SCOPE_PARAM))));
        context.failure(AuthenticationFlowError.USER_DISABLED, new ResponseBuilderImpl()
                .status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(new DockerErrorResponseToken(Collections.singletonList(error)))
                .build());
    }

    /**
     * For Docker protocol the same error message will be returned for invalid credentials and incorrect user name.  For SAML
     * ECP, there is a different behavior for each.
     */
    private void invalidUserAction(final AuthenticationFlowContext context, final RealmModel realm, final String userId, final Locale locale) {
        context.getEvent().user(userId);
        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);

        final DockerError error = new DockerError("UNAUTHORIZED","Invalid username or password.",
                Collections.singletonList(new DockerAccess(context.getAuthenticationSession().getClientNote(DockerAuthV2Protocol.SCOPE_PARAM))));

        context.failure(AuthenticationFlowError.INVALID_USER, new ResponseBuilderImpl()
                .status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(new DockerErrorResponseToken(Collections.singletonList(error)))
                .build());
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }
}
