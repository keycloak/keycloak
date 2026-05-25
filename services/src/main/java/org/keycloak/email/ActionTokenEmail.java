/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.email;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

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

/**
 * Shared helper for sending an "execute actions" email to a user.
 * <p>
 * Centralises the logic that was previously inlined in
 * {@code UserResource.executeActionsEmail()} and that workflow steps (e.g.
 * {@code InviteUserStepProvider}) would otherwise have to copy:
 * <ul>
 *   <li>resolving the target client (explicit {@code clientId} or the realm
 *   {@linkplain SystemClientUtil#getSystemClient(RealmModel) system client});</li>
 *   <li>validating the optional redirect URI against the resolved client;</li>
 *   <li>building the action-token URL and serialising the token;</li>
 *   <li>invoking {@link EmailTemplateProvider#sendExecuteActions(String, long)}
 *   with the standard template attributes.</li>
 * </ul>
 * The helper is intentionally HTTP-agnostic: it returns rich {@link Result}
 * values and throws {@link EmailException} / {@link IllegalArgumentException}
 * so callers can map outcomes onto either HTTP status codes (admin endpoints)
 * or silent skips (background workflow executors).
 */
public final class ActionTokenEmail {

    private ActionTokenEmail() {
    }

    /**
     * Resolved parameters for a single {@code execute-actions} email.
     */
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

    /**
     * Outcomes of {@link #resolveParams} that indicate a user is ineligible
     * for an action-token email. These are <em>not</em> errors: admin endpoints
     * may translate them into {@code 400 Bad Request}, while background
     * executors may silently skip the user.
     */
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

    /**
     * Result of {@link #resolveParams}: either successfully resolved
     * {@link Params} or a structured {@link Ineligibility} reason.
     */
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
     * Resolves the parameters needed to send an action-token email to
     * {@code user}. Returns a structured {@link Result} so callers can
     * differentiate between
     * <ul>
     *   <li>successful resolution → {@link Params};</li>
     *   <li>user/client/redirect ineligibility → {@link Ineligibility};</li>
     *   <li>caller-supplied invalid configuration (e.g. {@code redirectUri}
     *   without a {@code clientId}) → {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * @param session                 the Keycloak session
     * @param realm                   the realm the user belongs to
     * @param user                    the recipient
     * @param clientId                explicit client id, or {@code null} for the realm system client
     * @param redirectUri             optional post-action redirect URI; requires {@code clientId} when supplied
     * @param overrideLifespanSeconds explicit lifespan override; {@code null} falls back to
     *                                {@link RealmModel#getActionTokenGeneratedByAdminLifespan()}
     * @throws IllegalArgumentException when {@code redirectUri} is supplied without {@code clientId}
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
            resolvedRedirectUri = RedirectUtils.verifyRedirectUri(session, resolvedRedirectUri, client);
            if (resolvedRedirectUri == null) {
                return new Result(null, Ineligibility.INVALID_REDIRECT_URI);
            }
        }

        int lifespan = overrideLifespanSeconds != null
                ? overrideLifespanSeconds
                : realm.getActionTokenGeneratedByAdminLifespan();

        return new Result(new Params(client, resolvedRedirectUri, lifespan), null);
    }

    /**
     * Builds the action-token URL using the active HTTP request's base URI and
     * sends the email. Equivalent to {@link #send(KeycloakSession, RealmModel,
     * UserModel, Params, List, URI)} with {@code uriInfo.getBaseUri()}.
     */
    public static void send(KeycloakSession session, RealmModel realm, UserModel user,
                            Params params, List<String> actions, UriInfo uriInfo) throws EmailException {
        send(session, realm, user, params, actions, uriInfo.getBaseUri());
    }

    /**
     * Builds the action-token URL using {@code baseUri} and sends the email.
     * <p>
     * Use this overload from contexts that do not have an active HTTP request
     * (e.g. workflow executors) and therefore cannot supply a {@link UriInfo}.
     */
    public static void send(KeycloakSession session, RealmModel realm, UserModel user,
                            Params params, List<String> actions, URI baseUri) throws EmailException {
        int expiration = Time.currentTime() + params.lifespanSeconds;
        ExecuteActionsActionToken token = new ExecuteActionsActionToken(
                user.getId(), user.getEmail(), expiration, actions,
                params.redirectUri, params.client.getClientId());

        UriBuilder builder = Urls.loginActionsBase(baseUri)
                .path(LoginActionsService.class, "executeActionToken");
        String link = builder
                .queryParam("key", token.serialize(session, realm, baseUri))
                .build(realm.getName())
                .toString();

        session.getProvider(EmailTemplateProvider.class)
                .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
                .setAttribute(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER, true)
                .setRealm(realm)
                .setUser(user)
                .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(params.lifespanSeconds));
    }
}
