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

package org.keycloak.authentication.authenticators.browser;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SpnegoAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator{
    private static final Logger logger = Logger.getLogger(SpnegoAuthenticator.class);

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.attempted();
        return;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        HttpRequest request = context.getHttpRequest();
        String authHeader = request.getHttpHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null) {
            if (context.getAuthenticationSession().getAuthNote(AuthenticationProcessor.FORKED_FROM) != null) {
                // skip spnego authentication if it was forked (reset-credentials)
                context.attempted();
                return;
            }
            Response challenge = challengeNegotiation(context, null);
            context.forceChallenge(challenge);
            return;
        }

        String[] tokens = authHeader.split(" ");
        if (tokens.length == 0) { // assume not supported
            logger.debug("Invalid length of tokens: " + tokens.length);
            context.attempted();
            return;
        }
        if (!KerberosConstants.NEGOTIATE.equalsIgnoreCase(tokens[0])) {
            logger.debug("Unknown scheme " + tokens[0]);
            context.attempted();
            return;
        }
        if (tokens.length != 2) {
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
            return;
        }

        String spnegoToken = tokens[1];
        UserCredentialModel spnegoCredential = UserCredentialModel.kerberos(spnegoToken);

        CredentialValidationOutput output = context.getSession().users().getUserByCredential(context.getRealm(), spnegoCredential);

        if (output == null) {
            logger.warn("Received kerberos token, but there is no user storage provider that handles kerberos credentials.");
            context.attempted();
            return;
        }
        if (output.getAuthStatus() == CredentialValidationOutput.Status.AUTHENTICATED) {
            context.setUser(output.getAuthenticatedUser());
            if (output.getState() != null && !output.getState().isEmpty()) {
                for (Map.Entry<String, String> entry : output.getState().entrySet()) {
                    context.getAuthenticationSession().setUserSessionNote(entry.getKey(), entry.getValue());
                }
            }
            context.success(UserCredentialModel.KERBEROS);
        } else if (output.getAuthStatus() == CredentialValidationOutput.Status.CONTINUE) {
            String spnegoResponseToken = (String) output.getState().get(KerberosConstants.RESPONSE_TOKEN);
            Response challenge =  challengeNegotiation(context, spnegoResponseToken);
            context.challenge(challenge);
        } else {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
        }
    }

    private Response challengeNegotiation(AuthenticationFlowContext context, final String negotiateToken) {
        String negotiateHeader = negotiateToken == null ? KerberosConstants.NEGOTIATE : KerberosConstants.NEGOTIATE + " " + negotiateToken;

        if (logger.isTraceEnabled()) {
            logger.trace("Sending back " + HttpHeaders.WWW_AUTHENTICATE + ": " + negotiateHeader);
        }
        if (context.getExecution().isRequired()) {
            return context.getSession().getProvider(LoginFormsProvider.class)
                    .setAuthenticationSession(context.getAuthenticationSession())
                    .setResponseHeader(HttpHeaders.WWW_AUTHENTICATE, negotiateHeader)
                    .setError(Messages.KERBEROS_NOT_ENABLED).createErrorPage(Response.Status.UNAUTHORIZED);
        } else {
            return optionalChallengeRedirect(context, negotiateHeader);
        }
    }

    // This is used for testing only.  Selenium will execute the HTML challenge sent back which results in the javascript
    // redirecting.  Our old Selenium tests expect that the current URL will be the original openid redirect.
    public static boolean bypassChallengeJavascript = false;

    /**
     * 401 challenge sent back that bypasses
     * @param context
     * @param negotiateHeader
     * @return
     */
    protected Response optionalChallengeRedirect(AuthenticationFlowContext context, String negotiateHeader) {
        String accessCode = context.generateAccessCode();
        URI action = context.getActionUrl(accessCode);

        StringBuilder builder = new StringBuilder();

        builder.append("<HTML>");
        builder.append("<HEAD>");

        builder.append("<TITLE>Kerberos Unsupported</TITLE>");
        builder.append("</HEAD>");
        if (bypassChallengeJavascript) {
            builder.append("<BODY>");

        } else {
            builder.append("<BODY Onload=\"document.forms[0].submit()\">");
        }
        builder.append("<FORM METHOD=\"POST\" ACTION=\"" + action.toString() + "\">");
        builder.append("<NOSCRIPT>");
        builder.append("<P>JavaScript is disabled. We strongly recommend to enable it. You were unable to login via Kerberos.  Click the button below to login via an alternative method .</P>");
        builder.append("<INPUT name=\"continue\" TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />");
        builder.append("</NOSCRIPT>");

        builder.append("</FORM></BODY></HTML>");
        return Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, negotiateHeader)
                .type(MediaType.TEXT_HTML_TYPE)
                .entity(builder.toString()).build();
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
