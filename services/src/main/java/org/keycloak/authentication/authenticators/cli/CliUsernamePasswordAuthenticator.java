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

package org.keycloak.authentication.authenticators.cli;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CliUsernamePasswordAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String header = getHeader(context);
        Response response  = context.form()
                .setStatus(Response.Status.UNAUTHORIZED)
                .setMediaType(MediaType.TEXT_PLAIN_TYPE)
                .setResponseHeader(HttpHeaders.WWW_AUTHENTICATE, header)
                .createForm("cli_splash.ftl");
        context.challenge(response);


    }

    private String getHeader(AuthenticationFlowContext context) {
        URI callback = getCallbackUrl(context);
        return "X-Text-Form-Challenge callback=\"" + callback + "\" param=\"username\" label=\"Username: \" mask=false param=\"password\" label=\"Password: \" mask=true";
    }

    private URI getCallbackUrl(AuthenticationFlowContext context) {
        return context.getActionUrl(context.generateAccessCode(), true);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error) {
        String header = getHeader(context);
        Response response  = Response.status(401)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .header(HttpHeaders.WWW_AUTHENTICATE, header)
                .entity("\n" + context.form().getMessage(error) + "\n")
                .build();
        return response;
    }

    @Override
    protected Response setDuplicateUserChallenge(AuthenticationFlowContext context, String eventError, String loginFormError, AuthenticationFlowError authenticatorError) {
        context.getEvent().error(eventError);
        String header = getHeader(context);
        Response challengeResponse  = Response.status(401)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .header(HttpHeaders.WWW_AUTHENTICATE, header)
                .entity("\n" + context.form().getMessage(loginFormError) + "\n")
                .build();

        context.failureChallenge(authenticatorError, challengeResponse);
        return challengeResponse;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (!validateUserAndPassword(context, formData)) {
            return;
        }

        context.success();
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
}
