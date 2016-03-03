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

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VerifyEmail implements RequiredActionProvider, RequiredActionFactory {
    protected static ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().isVerifyEmail() && !context.getUser().isEmailVerified()) {
            context.getUser().addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
            logger.debug("User is required to verify email");
        }
    }
    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        if (context.getUser().isEmailVerified()) {
            context.success();
            return;
        }

        if (Validation.isBlank(context.getUser().getEmail())) {
            context.ignore();
            return;
        }

        context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, context.getUser().getEmail()).success();
        LoginActionsService.createActionCookie(context.getRealm(), context.getUriInfo(), context.getConnection(), context.getUserSession().getId());

        setupKey(context.getClientSession());

        LoginFormsProvider loginFormsProvider = context.getSession().getProvider(LoginFormsProvider.class)
                .setClientSessionCode(context.generateCode())
                .setClientSession(context.getClientSession())
                .setUser(context.getUser());
        Response challenge = loginFormsProvider.createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
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

    public static void setupKey(ClientSessionModel clientSession) {
        String secret = HmacOTP.generateSecret(10);
        clientSession.setNote(Constants.VERIFY_EMAIL_KEY, secret);
    }
}
