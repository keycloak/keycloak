package org.keycloak.protocol.saml.profile.ecp.authenticator;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.common.util.Base64;
import org.keycloak.events.Errors;
import org.keycloak.models.*;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpBasicAuthenticator implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "http-basic-authenticator";

    @Override
    public String getDisplayType() {
        return null;
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return new Requirement[0];
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new Authenticator() {

            private static final String BASIC = "Basic";
            private static final String BASIC_PREFIX = BASIC + " ";

            @Override
            public void authenticate(AuthenticationFlowContext context) {
                HttpRequest httpRequest = context.getHttpRequest();
                HttpHeaders httpHeaders = httpRequest.getHttpHeaders();
                String[] usernameAndPassword = getUsernameAndPassword(httpHeaders);

                context.attempted();

                if (usernameAndPassword != null) {
                    RealmModel realm = context.getRealm();
                    UserModel user = context.getSession().users().getUserByUsername(usernameAndPassword[0], realm);

                    if (user != null) {
                        String password = usernameAndPassword[1];
                        boolean valid = context.getSession().users().validCredentials(context.getSession(), realm, user, UserCredentialModel.password(password));

                        if (valid) {
                            context.getClientSession().setAuthenticatedUser(user);
                            context.success();
                        } else {
                            context.getEvent().user(user);
                            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
                            context.failure(AuthenticationFlowError.INVALID_USER, Response.status(Response.Status.UNAUTHORIZED)
                                    .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_PREFIX + "realm=\"" + realm.getName() + "\"")
                                    .build());
                        }
                    }
                }
            }

            private String[] getUsernameAndPassword(HttpHeaders httpHeaders) {
                List<String> authHeaders = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);

                if (authHeaders == null || authHeaders.size() == 0) {
                    return null;
                }

                String credentials = null;

                for (String authHeader : authHeaders) {
                    if (authHeader.startsWith(BASIC_PREFIX)) {
                        String[] split = authHeader.trim().split("\\s+");

                        if (split == null || split.length != 2) return null;

                        credentials = split[1];
                    }
                }

                try {
                    return new String(Base64.decode(credentials)).split(":");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse credentials.", e);
                }
            }

            @Override
            public void action(AuthenticationFlowContext context) {

            }

            @Override
            public boolean requiresUser() {
                return false;
            }

            @Override
            public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
                return false;
            }

            @Override
            public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
