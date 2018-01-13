/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.authentication;

import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PushButtonAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String accessCode = context.generateAccessCode();
        String actionUrl = context.getActionUrl(accessCode).toString();

        StringBuilder response = new StringBuilder("<html><head><title>PushTheButton</title></head><body>");

        UserModel user = context.getUser();
        if (user == null) {
            response.append("No authenticated user<br>");
        } else {
            response.append("Authenticated user: " + user.getUsername() + "<br>");
        }

        response.append("<form method='POST' action='" + actionUrl + "'>");
        response.append(" This is the Test Approver. Press login to continue.<br>");
        response.append(" <input type='submit' name='submit1' value='Submit' />");
        response.append("</form></body></html>");
        String html = response.toString();

        Response jaxrsResponse = Response
                .status(Response.Status.OK)
                .type("text/html")
                .entity(html)
                .build();

        context.challenge(jaxrsResponse);

//        Response challenge = context.form().createForm("login-approve.ftl");
//        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.success();
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
}
