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

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.UpdateTotp;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.credential.OTPCredentialProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OTPFormAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator, CredentialValidator<OTPCredentialProvider> {

    // Freemarker attribute where selected OTP credential will be stored
    public static final String SELECTED_OTP_CREDENTIAL_ID = "selectedOtpCredentialId";

    // Label to be shown in the UI for the "unnamed" OTP credential, which doesn't have userLabel
    public static final String UNNAMED = "unnamed";


    @Override
    public void action(AuthenticationFlowContext context) {
        validateOTP(context);
    }


    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response challengeResponse = challenge(context, null);
        context.challenge(challengeResponse);
    }


    public void validateOTP(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();

        String otp = inputData.getFirst("otp");

        String credentialId = inputData.getFirst("selectedCredentialId");

        if (credentialId == null || credentialId.isEmpty()) {
            OTPCredentialModel defaultOtpCredential = getCredentialProvider(context.getSession())
                    .getDefaultCredential(context.getSession(), context.getRealm(), context.getUser());
            credentialId = defaultOtpCredential==null ? "" : defaultOtpCredential.getId();
        }
        context.getEvent().detail(Details.SELECTED_CREDENTIAL_ID, credentialId);

        context.form().setAttribute(SELECTED_OTP_CREDENTIAL_ID, credentialId);

        UserModel userModel = context.getUser();
        if (!enabledUser(context, userModel)) {
            // error in context is set in enabledUser/isDisabledByBruteForce
            return;
        }

        if (otp == null) {
            Response challengeResponse = challenge(context,null);
            context.challenge(challengeResponse);
            return;
        }
        boolean valid = context.getSession().userCredentialManager().isValid(context.getRealm(),context.getUser(),
                new UserCredentialModel(credentialId, getCredentialProvider(context.getSession()).getType(), otp));
        if (!valid) {
            context.getEvent().user(userModel)
                    .error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = challenge(context, Messages.INVALID_TOTP, Validation.FIELD_OTP_CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return;
        }
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    protected String disabledByBruteForceError() {
        return Messages.INVALID_TOTP;
    }

    @Override
    protected String disabledByBruteForceFieldError() {
        return Validation.FIELD_OTP_CODE;
    }

    @Override
    protected Response createLoginForm(LoginFormsProvider form) {
        return form.createLoginTotp();
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, getCredentialProvider(session).getType());
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();
        if (!authenticationSession.getRequiredActions().contains(UserModel.RequiredAction.CONFIGURE_TOTP.name())) {
            authenticationSession.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        }
    }

    public List<RequiredActionFactory> getRequiredActions(KeycloakSession session) {
        return Collections.singletonList((UpdateTotp)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, UserModel.RequiredAction.CONFIGURE_TOTP.name()));
    }

    @Override
    public void close() {

    }

    @Override
    public OTPCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (OTPCredentialProvider)session.getProvider(CredentialProvider.class, OTPCredentialProviderFactory.PROVIDER_ID);
    }

}
