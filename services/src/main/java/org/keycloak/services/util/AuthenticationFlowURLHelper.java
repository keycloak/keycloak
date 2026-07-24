/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.util;

import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationFlowURLHelper {

    protected static final Logger logger = Logger.getLogger(AuthenticationFlowURLHelper.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UriInfo uriInfo;

    public AuthenticationFlowURLHelper(KeycloakSession session, RealmModel realm, UriInfo uriInfo) {
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
    }


    public Response showPageExpired(AuthenticationSessionModel authSession) {
        URI lastStepUrl = getLastExecutionUrl(authSession);

        logger.debugf("Redirecting to 'page expired' now. Will use last step URL: %s", lastStepUrl);

        LocaleUtil.processLocaleParam(session, realm, authSession);
        return session.getProvider(LoginFormsProvider.class).setAuthenticationSession(authSession)
                .setActionUri(lastStepUrl)
                .setExecution(getExecutionId(authSession))
                .createLoginExpiredPage();
    }


    public URI getLastExecutionUrl(String flowPath, String executionId, String clientId, String tabId, String clientData) {
        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(flowPath);

        if (executionId != null) {
            uriBuilder.queryParam(Constants.EXECUTION, executionId);
        }
        uriBuilder.queryParam(Constants.CLIENT_ID, clientId);
        uriBuilder.queryParam(Constants.TAB_ID, tabId);
        uriBuilder.queryParam(Constants.CLIENT_DATA, clientData);

        return uriBuilder.build(realm.getName());
    }


    public URI getLastExecutionUrl(AuthenticationSessionModel authSession) {
        String executionId = getExecutionId(authSession);
        String latestFlowPath = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);

        if (latestFlowPath == null) {
            latestFlowPath = authSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
        }

        if (latestFlowPath == null) {
            latestFlowPath = LoginActionsService.AUTHENTICATE_PATH;
        }

        String clientData = AuthenticationProcessor.getClientData(session, authSession);
        return getLastExecutionUrl(latestFlowPath, executionId, authSession.getClient().getClientId(), authSession.getTabId(), clientData);
    }

    private String getExecutionId(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
    }

}
