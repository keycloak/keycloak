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

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.resources.LoginActionsService;

import org.jboss.logging.Logger;

public class InviteUserStepProvider implements WorkflowStepProvider {

    public static final String CONFIG_ACTIONS = "actions";
    public static final String CONFIG_CLIENT_ID = "client-id";
    public static final String CONFIG_REDIRECT_URI = "redirect-uri";

    private static final List<String> DEFAULT_ACTIONS = List.of(
            UserModel.RequiredAction.UPDATE_PASSWORD.name(),
            UserModel.RequiredAction.VERIFY_EMAIL.name()
    );

    private final KeycloakSession session;
    private final ComponentModel stepModel;
    private final Logger log = Logger.getLogger(InviteUserStepProvider.class);

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

        if (user == null || user.getEmail() == null || !user.isEnabled()) {
            return;
        }

        String clientId = stepModel.getConfig().getFirst(CONFIG_CLIENT_ID);
        ClientModel client = clientId == null
                ? SystemClientUtil.getSystemClient(realm)
                : realm.getClientByClientId(clientId);

        if (client == null || !client.isEnabled()) {
            return;
        }

        String redirectUri = stepModel.getConfig().getFirst(CONFIG_REDIRECT_URI);
        if (redirectUri != null) {
            redirectUri = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
            if (redirectUri == null) {
                return;
            }
        }

        UriInfo uriInfo = resolveUriInfo();
        if (uriInfo == null) {
            return;
        }

        List<String> actions = stepModel.getConfig().getOrDefault(CONFIG_ACTIONS, DEFAULT_ACTIONS);
        int lifespan = realm.getActionTokenGeneratedByAdminLifespan();
        int expiration = Time.currentTime() + lifespan;

        ExecuteActionsActionToken token = new ExecuteActionsActionToken(
                user.getId(), user.getEmail(), expiration, actions, redirectUri, client.getClientId());

        String link = LoginActionsService.actionTokenProcessor(uriInfo)
                .queryParam("key", token.serialize(session, realm, uriInfo))
                .build(realm.getName())
                .toString();

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
                    .setAttribute(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER, true)
                    .setRealm(realm)
                    .setUser(user)
                    .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(lifespan));
        } catch (EmailException e) {
            log.errorv(e, "Failed to send invite email to user {0} ({1})", user.getUsername(), user.getEmail());
        }
    }

    private UriInfo resolveUriInfo() {
        try {
            return session.getContext().getUri();
        } catch (NullPointerException e) {
            // No active HTTP request and neither --hostname=<full URL> nor realm frontendUrl set:
            // KeycloakUriInfo cannot resolve a base URI. Skip rather than crash the workflow.
            log.warn("Cannot build invite link without --hostname or realm frontendUrl configured");
            return null;
        }
    }
}
