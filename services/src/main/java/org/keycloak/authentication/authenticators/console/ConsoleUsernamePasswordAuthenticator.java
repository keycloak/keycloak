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

package org.keycloak.authentication.authenticators.console;

import org.keycloak.authentication.*;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConsoleUsernamePasswordAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    public static final ConsoleUsernamePasswordAuthenticator SINGLETON = new ConsoleUsernamePasswordAuthenticator();

    @Override
    public boolean requiresUser() {
        return false;
    }

    protected ConsoleDisplayMode challenge(AuthenticationFlowContext context) {
        return ConsoleDisplayMode.challenge(context)
                .header()
                .param("username")
                .label("console-username")
                .param("password")
                .label("console-password")
                .mask(true)
                .challenge();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response response = challenge(context).form().createForm("cli_splash.ftl");
        context.challenge(response);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error) {
        return challenge(context).message(error);
    }

    @Override
    protected Response setDuplicateUserChallenge(AuthenticationFlowContext context, String eventError, String loginFormError, AuthenticationFlowError authenticatorError) {
        context.getEvent().error(eventError);
        Response response = challenge(context).message(loginFormError);

        context.failureChallenge(authenticatorError, response);
        return response;
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
