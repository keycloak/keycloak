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

package org.keycloak.models.workflow;

import java.net.URI;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.email.ActionTokenEmail;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;

import org.jboss.logging.Logger;

public class InviteUserStepProvider implements WorkflowStepProvider {

    public static final String CONFIG_ACTIONS = "actions";
    public static final String CONFIG_CLIENT_ID = "client-id";
    public static final String CONFIG_REDIRECT_URI = "redirect-uri";

    /**
     * Operator-facing message describing the configuration that this step requires.
     * Used by both runtime warnings ({@link #run}) and configuration validation
     * ({@link InviteUserStepProviderFactory#validateConfiguration}).
     */
    static final String HOSTNAME_NOT_CONFIGURED_MESSAGE =
            "invite-user requires a configured Keycloak hostname: set --hostname=<full URL> "
                    + "or the realm 'frontendUrl' attribute";

    private static final List<String> DEFAULT_ACTIONS = List.of(
            UserModel.RequiredAction.UPDATE_PASSWORD.name(),
            UserModel.RequiredAction.VERIFY_EMAIL.name()
    );

    private static final Logger LOG = Logger.getLogger(InviteUserStepProvider.class);

    private final KeycloakSession session;
    private final ComponentModel stepModel;

    public InviteUserStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.stepModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());
        if (user == null) {
            return;
        }

        String clientId = stepModel.getConfig().getFirst(CONFIG_CLIENT_ID);
        String redirectUri = stepModel.getConfig().getFirst(CONFIG_REDIRECT_URI);

        ActionTokenEmail.Result resolved;
        try {
            resolved = ActionTokenEmail.resolveParams(session, realm, user, clientId, redirectUri, null);
        } catch (IllegalArgumentException e) {
            // Caller-supplied invalid configuration. The factory validates this at create
            // time, so reaching here means the configuration was changed after creation.
            LOG.warnf("Skipping invite for user %s: %s", user.getUsername(), e.getMessage());
            return;
        }
        if (resolved.getParams().isEmpty()) {
            // User/client/redirect ineligible — silently skip, this is normal for workflows
            // (e.g. user has no email, account disabled, etc.).
            return;
        }

        URI baseUri = resolveBaseUri(session);
        if (baseUri == null) {
            // Should be unreachable because the factory rejects this configuration up-front,
            // but keep a defensive log in case hostname configuration changes at runtime.
            LOG.warnf("Skipping invite for user %s: %s", user.getUsername(), HOSTNAME_NOT_CONFIGURED_MESSAGE);
            return;
        }

        List<String> actions = stepModel.getConfig().getOrDefault(CONFIG_ACTIONS, DEFAULT_ACTIONS);

        try {
            ActionTokenEmail.send(session, realm, user, resolved.getParams().get(), actions, baseUri);
        } catch (EmailException e) {
            LOG.errorv(e, "Failed to send invite email to user {0} ({1})", user.getUsername(), user.getEmail());
        }
    }

    /**
     * Resolves the Keycloak base URI suitable for building absolute action-token URLs.
     * <p>
     * Workflow steps may run on a thread without an active HTTP request, so we cannot
     * rely on {@link org.keycloak.models.KeycloakContext#getUri()} (it is request-scoped).
     * The {@link HostnameProvider} can still produce a base URI provided that
     * {@code --hostname=<full URL>} or the realm {@code frontendUrl} attribute is
     * configured; otherwise it has no fallback and indicates this by throwing a
     * {@link NullPointerException} when called with a {@code null} {@code originalUriInfo}.
     * <p>
     * Note: {@code org.keycloak.ssf.transmitter.support.SsfUtil#getIssuerUrl} solves a
     * closely related problem for the Shared Signals Framework, with its own fallback
     * chain (realm {@code frontendUrl} → {@code KC_HOSTNAME_URL} env → {@code hostname}
     * config → request URI). Unifying both into a single public helper would be a useful
     * follow-up refactoring but is intentionally out of scope for this change.
     *
     * @return the resolved base URI, or {@code null} when no static hostname is configured
     */
    static URI resolveBaseUri(KeycloakSession session) {
        try {
            return session.getProvider(HostnameProvider.class).getBaseUri(null, UrlType.FRONTEND);
        } catch (NullPointerException e) {
            LOG.debugv(e, "HostnameProvider could not resolve a base URI without an active request");
            return null;
        }
    }
}
