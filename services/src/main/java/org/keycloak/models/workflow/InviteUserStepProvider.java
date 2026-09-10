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

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.email.ActionTokenEmail;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

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
            "invite-user requires a configured Keycloak hostname: set the realm 'frontendUrl' "
                    + "attribute or --hostname=<full URL>";

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
            // Ineligible recipients (no email, disabled account, unavailable client, invalid
            // redirect) are a normal skip; log the reason at debug level for diagnosability.
            LOG.debugf("Skipping invite for user %s: %s", user.getUsername(),
                    resolved.getIneligibility().map(Enum::name).orElse("ineligible"));
            return;
        }

        URI baseUri = resolveBaseUri(realm);
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
     * Resolves the Keycloak base URI for building absolute action-token URLs from static
     * configuration only, checking in order: the realm {@code frontendUrl} attribute, then the
     * {@code --hostname} option ({@code KC_HOSTNAME}, resolved through the Quarkus config chain)
     * when it is set to a full URL. Request-scoped resolution is unavailable here because the
     * step runs on a background executor thread with no active HTTP request.
     *
     * @param realm the realm the invitation targets; its {@code frontendUrl} attribute is
     *              preferred over the server-wide hostname configuration
     * @return the resolved base URI, or {@code null} when no static hostname is configured
     */
    static URI resolveBaseUri(RealmModel realm) {
        URI baseUri = parseBaseUri(realm == null ? null : realm.getAttribute("frontendUrl"));
        if (baseUri != null) {
            return baseUri;
        }

        // '--hostname' may be either a bare host name or a full URL. Only a full URL carries
        // the scheme/port needed to build absolute links without an active request.
        String configuredHostname = Config.scope("hostname", "v2").get("hostname");
        if (configuredHostname != null
                && (configuredHostname.startsWith("http://") || configuredHostname.startsWith("https://"))) {
            return parseBaseUri(configuredHostname);
        }

        return null;
    }

    private static URI parseBaseUri(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            LOG.warnf("Ignoring invalid base URL '%s' when resolving invite-user links: %s", url, e.getMessage());
            return null;
        }
        boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        if (!uri.isAbsolute() || !httpScheme || uri.getHost() == null) {
            LOG.warnf("Ignoring base URL '%s' when resolving invite-user links: expected an absolute http(s) URL", url);
            return null;
        }
        return uri;
    }
}
