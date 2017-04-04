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

package org.keycloak.authentication.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.services.Urls;
import org.keycloak.services.validation.Validation;

import org.keycloak.sessions.AuthenticationSessionModel;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VerifyEmail implements RequiredActionProvider, RequiredActionFactory {
    private static final Logger logger = Logger.getLogger(VerifyEmail.class);
    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().isVerifyEmail() && !context.getUser().isEmailVerified()) {
            context.getUser().addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
            logger.debug("User is required to verify email");
        }
    }
    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (context.getUser().isEmailVerified()) {
            context.success();
            authSession.removeAuthNote(Constants.VERIFY_EMAIL_KEY);
            return;
        }

        String email = context.getUser().getEmail();
        if (Validation.isBlank(email)) {
            context.ignore();
            return;
        }

        LoginFormsProvider loginFormsProvider = context.getSession().getProvider(LoginFormsProvider.class)
                .setClientSessionCode(context.generateCode())
                .setAuthenticationSession(authSession)
                .setUser(context.getUser());
        Response challenge;

        // Do not allow resending e-mail by simple page refresh, i.e. when e-mail sent, it should be resent properly via email-verification endpoint
        if (! Objects.equals(authSession.getAuthNote(Constants.VERIFY_EMAIL_KEY), email)) {
            authSession.setAuthNote(Constants.VERIFY_EMAIL_KEY, email);
            context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, email).success();
            challenge = sendVerifyEmail(context.getSession(), context.generateCode(), context.getUser(), context.getAuthenticationSession());
        } else {
            challenge = loginFormsProvider.createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
        }

        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        context.failure();
    }


    @Override
    public void close() {

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getDisplayText() {
        return "Verify Email";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.VERIFY_EMAIL.name();
    }

    public static Response sendVerifyEmail(KeycloakSession session, String clientCode, UserModel user, AuthenticationSessionModel authSession) throws UriBuilderException, IllegalArgumentException {
        RealmModel realm = session.getContext().getRealm();
        UriInfo uriInfo = session.getContext().getUri();

        LoginFormsProvider forms = session.getProvider(LoginFormsProvider.class)
          .setClientSessionCode(clientCode)
          .setAuthenticationSession(authSession)
          .setUser(authSession.getAuthenticatedUser());

        int validityInSecs = realm.getAccessCodeLifespanUserAction();
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
//        ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), absoluteExpirationInSecs, null,
//          Collections.singletonList(UserModel.RequiredAction.VERIFY_EMAIL.name()),
//          null, null);
//        token.setAuthenticationSessionId(authenticationSession.getId());

        VerifyEmailActionToken token = new VerifyEmailActionToken(user.getId(), absoluteExpirationInSecs, null, authSession.getId(), user.getEmail());
        UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo));
        String link = builder.build(realm.getName()).toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);

        try {
            session.getProvider(EmailTemplateProvider.class).setRealm(realm).setUser(user).sendVerifyEmail(link, expirationInMinutes);
        } catch (EmailException e) {
            logger.error("Failed to send verification email", e);
            return forms.createResponse(RequiredAction.VERIFY_EMAIL);
        }

        return forms.createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
    }

    public static void setupKey(AuthenticationSessionModel session) {
        String secret = HmacOTP.generateSecret(10);
        session.setAuthNote(Constants.VERIFY_EMAIL_KEY, secret);
    }
}
