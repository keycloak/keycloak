package org.keycloak.email;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.LoginActionsService;

import org.jboss.logging.Logger;

/**
 * Sends an action-token email driving a user through a set of required actions.
 */
public final class ActionTokenEmail {

    private static final Logger LOG = Logger.getLogger(ActionTokenEmail.class);

    private ActionTokenEmail() {
    }

    /** Parameters for an action-token email. */
    public static final class Params {
        private final ClientModel client;
        private final String redirectUri;
        private final int lifespanSeconds;

        Params(ClientModel client, String redirectUri, int lifespanSeconds) {
            this.client = client;
            this.redirectUri = redirectUri;
            this.lifespanSeconds = lifespanSeconds;
        }

        public ClientModel getClient() {
            return client;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public int getLifespanSeconds() {
            return lifespanSeconds;
        }
    }

    /** Reasons a user is not eligible for an action-token email. */
    public enum Ineligibility {
        /** The user has no email address set. */
        USER_HAS_NO_EMAIL,
        /** The user account is disabled. */
        USER_DISABLED,
        /** The resolved client does not exist or is not enabled. */
        CLIENT_UNAVAILABLE,
        /** The supplied {@code redirectUri} is not valid for the resolved client. */
        INVALID_REDIRECT_URI,
    }

    /** Resolved {@link Params}, or an {@link Ineligibility} reason. */
    public static final class Result {
        private final Params params;
        private final Ineligibility ineligibility;

        private Result(Params params, Ineligibility ineligibility) {
            this.params = params;
            this.ineligibility = ineligibility;
        }

        public Optional<Params> getParams() {
            return Optional.ofNullable(params);
        }

        public Optional<Ineligibility> getIneligibility() {
            return Optional.ofNullable(ineligibility);
        }
    }

    /**
     * Validates and resolves the parameters needed to send an action-token email.
     *
     * @param clientId explicit client id, or {@code null} to use the realm system client
     * @param redirectUri optional post-action redirect URI; requires {@code clientId}
     * @param overrideLifespanSeconds explicit lifespan, or {@code null} for the realm default
     * @throws IllegalArgumentException if {@code redirectUri} is set without {@code clientId}
     */
    public static Result resolveParams(KeycloakSession session, RealmModel realm, UserModel user,
                                       String clientId, String redirectUri,
                                       Integer overrideLifespanSeconds) {
        if (user.getEmail() == null) {
            return new Result(null, Ineligibility.USER_HAS_NO_EMAIL);
        }
        if (!user.isEnabled()) {
            return new Result(null, Ineligibility.USER_DISABLED);
        }
        if (redirectUri != null && clientId == null) {
            throw new IllegalArgumentException("'redirect-uri' requires 'client-id' to be set");
        }

        ClientModel client = clientId != null
                ? realm.getClientByClientId(clientId)
                : SystemClientUtil.getSystemClient(realm);
        if (client == null || !client.isEnabled()) {
            return new Result(null, Ineligibility.CLIENT_UNAVAILABLE);
        }

        String resolvedRedirectUri = redirectUri;
        if (resolvedRedirectUri != null) {
            try {
                resolvedRedirectUri = RedirectUtils.verifyRedirectUri(session, resolvedRedirectUri, client);
                if (resolvedRedirectUri == null) {
                    return new Result(null, Ineligibility.INVALID_REDIRECT_URI);
                }
            } catch (ContextNotActiveException e) {
                // No HTTP request context (e.g. workflow executor): defer redirect-uri validation
                // to the action-token handler at click time.
                LOG.debugf("Skipping early redirect-uri validation: %s", e.getMessage());
            }
        }

        int lifespan = overrideLifespanSeconds != null
                ? overrideLifespanSeconds
                : realm.getActionTokenGeneratedByAdminLifespan();

        return new Result(new Params(client, resolvedRedirectUri, lifespan), null);
    }

    /**
     * Sends an invitation email that drives the user through the given required actions via a
     * one-time action-token link.
     */
    public static void sendInviteUser(KeycloakSession session, RealmModel realm, UserModel user,
                                      Params params, List<String> actions, URI baseUri) throws EmailException {
        String link = buildActionTokenLink(session, realm, user, params, actions, baseUri);
        long expirationMinutes = TimeUnit.SECONDS.toMinutes(params.lifespanSeconds);
        prepareTemplateProvider(session, realm, user, actions)
                .sendInviteUserEmail(link, expirationMinutes);
    }

    private static String buildActionTokenLink(KeycloakSession session, RealmModel realm, UserModel user,
                                               Params params, List<String> actions, URI baseUri) {
        int expiration = Time.currentTime() + params.lifespanSeconds;
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(
                user.getId(), user.getEmail(), expiration, actions,
                params.redirectUri, params.client.getClientId());

        UriBuilder builder = Urls.loginActionsBase(baseUri)
                .path(LoginActionsService.class, "executeActionToken");
        return builder
                .queryParam("key", token.serialize(session, realm, baseUri))
                .build(realm.getName())
                .toString();
    }

    private static EmailTemplateProvider prepareTemplateProvider(KeycloakSession session, RealmModel realm,
                                                                 UserModel user, List<String> actions) {
        return session.getProvider(EmailTemplateProvider.class)
                .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, actions)
                .setAttribute(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER, true)
                .setRealm(realm)
                .setUser(user);
    }
}
