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

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.ConsoleDisplayMode;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.model.TotpBean;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConsoleUpdateTotp implements RequiredActionProvider {
    public static final ConsoleUpdateTotp SINGLETON = new ConsoleUpdateTotp();

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }
    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        TotpBean totpBean = new TotpBean(context.getSession(), context.getRealm(), context.getUser(), context.getUriInfo().getRequestUriBuilder());
        String totpSecret = totpBean.getTotpSecret();
        context.getAuthenticationSession().setAuthNote("totpSecret", totpSecret);
        Response challenge = challenge(context).form()
                .setAttribute("totp", totpBean)
                .createForm("login-config-totp-text.ftl");
        context.challenge(challenge);
    }

    protected ConsoleDisplayMode challenge(RequiredActionContext context) {
        return ConsoleDisplayMode.challenge(context)
                .header()
                .param("totp")
                .label("console-otp")
                .challenge();
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent();
        event.event(EventType.UPDATE_TOTP);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String totp = formData.getFirst("totp");
        String totpSecret = context.getAuthenticationSession().getAuthNote("totpSecret");

        if (Validation.isBlank(totp)) {
            context.challenge(
                    challenge(context).message(Messages.MISSING_TOTP)
            );
            return;
        } else if (!CredentialValidation.validOTP(context.getRealm(), totp, totpSecret)) {
            context.challenge(
                    challenge(context).message(Messages.INVALID_TOTP)
            );
            return;
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(context.getRealm().getOTPPolicy().getType());
        credentials.setValue(totpSecret);
        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);


        // if type is HOTP, to update counter we execute validation based on supplied token
        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(context.getRealm().getOTPPolicy().getType());
        cred.setValue(totp);
        context.getSession().userCredentialManager().isValid(context.getRealm(), context.getUser(), cred);

        context.getAuthenticationSession().removeAuthNote("totpSecret");
        context.success();
    }


    @Override
    public void close() {

    }
}
