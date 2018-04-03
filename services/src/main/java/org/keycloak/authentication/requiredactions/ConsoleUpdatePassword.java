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
import org.keycloak.authentication.*;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConsoleUpdatePassword extends UpdatePassword implements RequiredActionProvider {
    public static final ConsoleUpdatePassword SINGLETON = new ConsoleUpdatePassword();

    private static final Logger logger = Logger.getLogger(ConsoleUpdatePassword.class);
    public static final String PASSWORD_NEW = "password-new";
    public static final String PASSWORD_CONFIRM = "password-confirm";

     protected ConsoleDisplayMode challenge(RequiredActionContext context) {
        return ConsoleDisplayMode.challenge(context)
                .header()
                .param(PASSWORD_NEW)
                .label("console-new-password")
                .mask(true)
                .param(PASSWORD_CONFIRM)
                .label("console-confirm-password")
                .mask(true)
                .challenge();
    }



    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.challenge(
                challenge(context).message("console-update-password"));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        event.event(EventType.UPDATE_PASSWORD);
        String passwordNew = formData.getFirst(PASSWORD_NEW);
        String passwordConfirm = formData.getFirst(PASSWORD_CONFIRM);

        EventBuilder errorEvent = event.clone().event(EventType.UPDATE_PASSWORD_ERROR)
                .client(context.getAuthenticationSession().getClient())
                .user(context.getAuthenticationSession().getAuthenticatedUser());

        if (Validation.isBlank(passwordNew)) {
            context.challenge(challenge(context).message(Messages.MISSING_PASSWORD));
            errorEvent.error(Errors.PASSWORD_MISSING);
            return;
        } else if (!passwordNew.equals(passwordConfirm)) {
            context.challenge(challenge(context).message(Messages.NOTMATCH_PASSWORD));
            errorEvent.error(Errors.PASSWORD_CONFIRM_ERROR);
            return;
        }

        try {
            context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), UserCredentialModel.password(passwordNew, false));
            context.success();
        } catch (ModelException me) {
            errorEvent.detail(Details.REASON, me.getMessage()).error(Errors.PASSWORD_REJECTED);
            context.challenge(challenge(context).text(me.getMessage()));
            return;
        } catch (Exception ape) {
            errorEvent.detail(Details.REASON, ape.getMessage()).error(Errors.PASSWORD_REJECTED);
            context.challenge(challenge(context).text(ape.getMessage()));
            return;
        }
    }
}
