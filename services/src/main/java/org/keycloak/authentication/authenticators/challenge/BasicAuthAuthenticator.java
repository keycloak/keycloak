/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.challenge;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BasicAuthAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String authorizationHeader = getAuthorizationHeader(context);

        if (authorizationHeader == null) {
            if (context.getExecution().isRequired()) {
                context.challenge(challenge(context, null));
            } else {
                context.attempted();
            }
            return;
        }

        String[] challenge = getChallenge(authorizationHeader);

        if (challenge == null) {
            if (context.getExecution().isRequired()) {
                context.challenge(challenge(context, null));
            } else {
                context.attempted();
            }
            return;
        }

        if (onAuthenticate(context, challenge)) {
            context.success();
            return;
        }
    }

    protected boolean onAuthenticate(AuthenticationFlowContext context, String[] challenge) {
        if (checkUsernameAndPassword(context, challenge[0], challenge[1])) {
            return true;
        }

        return false;
    }

    protected String getAuthorizationHeader(AuthenticationFlowContext context) {
        return context.getHttpRequest().getHttpHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    }

    protected boolean checkUsernameAndPassword(AuthenticationFlowContext context, String username, String password) {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();

        map.putSingle(AuthenticationManager.FORM_USERNAME, username);
        map.putSingle(CredentialRepresentation.PASSWORD, password);

        if (validateUserAndPassword(context, map)) {
            return true;
        }

        return false;
    }

    protected String[] getChallenge(String authorizationHeader) {
        String[] challenge = BasicAuthHelper.parseHeader(authorizationHeader);

        if (challenge.length < 2) {
            return null;
        }

        return challenge;
    }

    @Override
    protected Response setDuplicateUserChallenge(AuthenticationFlowContext context, String eventError, String loginFormError, AuthenticationFlowError authenticatorError) {
        return challenge(context, null);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error) {
        return Response.status(401).header(HttpHeaders.WWW_AUTHENTICATE, getHeader(context)).build();
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        return challenge(context, error);
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }

    private String getHeader(AuthenticationFlowContext context) {
        return "Basic realm=\"" + context.getRealm().getName() + "\"";
    }
}