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

import org.jboss.logging.Logger;
import org.keycloak.authentication.AbstractFormAuthenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;
import static org.keycloak.services.validation.Validation.FIELD_PASSWORD;
import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractUsernameFormAuthenticator extends AbstractFormAuthenticator {

    private static final Logger logger = Logger.getLogger(AbstractUsernameFormAuthenticator.class);

    public static final String REGISTRATION_FORM_ACTION = "registration_form";
    public static final String ATTEMPTED_USERNAME = "ATTEMPTED_USERNAME";

    // Flag is true if user was already set in the authContext before this authenticator was triggered. In this case we skip clearing of the user after unsuccessful password authentication
    protected static final String USER_SET_BEFORE_USERNAME_PASSWORD_AUTH = "USER_SET_BEFORE_USERNAME_PASSWORD_AUTH";

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    protected Response challenge(AuthenticationFlowContext context, String error) {
        return challenge(context, error, null);
    }

    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        LoginFormsProvider form = context.form()
                .setExecution(context.getExecution().getId());
        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error));
            } else {
                form.setError(error);
            }
        }
        return createLoginForm(form);
    }

    protected Response createLoginForm(LoginFormsProvider form) {
        return form.createLoginUsernamePassword();
    }

    protected String disabledByBruteForceError() {
        return Messages.INVALID_USER;
    }

    protected String disabledByBruteForceFieldError(){
        return FIELD_USERNAME;
    }

    protected Response setDuplicateUserChallenge(AuthenticationFlowContext context, String eventError, String loginFormError, AuthenticationFlowError authenticatorError) {
        context.getEvent().error(eventError);
        Response challengeResponse = context.form()
                .setError(loginFormError).createLoginUsernamePassword();
        context.failureChallenge(authenticatorError, challengeResponse);
        return challengeResponse;
    }

    protected void runDefaultDummyHash(AuthenticationFlowContext context) {
        PasswordHashProvider hash = context.getSession().getProvider(PasswordHashProvider.class, PasswordPolicy.HASH_ALGORITHM_DEFAULT);
        hash.encode("SlightlyLongerDummyPassword", PasswordPolicy.HASH_ITERATIONS_DEFAULT);
    }

    protected void dummyHash(AuthenticationFlowContext context) {
        PasswordPolicy policy = context.getRealm().getPasswordPolicy();
        if (policy == null) {
            runDefaultDummyHash(context);
            return;
        } else {
            PasswordHashProvider hash = context.getSession().getProvider(PasswordHashProvider.class, policy.getHashAlgorithm());
            if (hash == null) {
                runDefaultDummyHash(context);
                return;

            } else {
                hash.encode("SlightlyLongerDummyPassword", policy.getHashIterations());
            }
        }

    }

    public void testInvalidUser(AuthenticationFlowContext context, UserModel user) {
        if (user == null) {
            dummyHash(context);
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_USERNAME);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
        }
    }

    public boolean enabledUser(AuthenticationFlowContext context, UserModel user) {
        if (isDisabledByBruteForce(context, user)) return false;
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(Errors.USER_DISABLED);
            Response challengeResponse = challenge(context, Messages.ACCOUNT_DISABLED);
            context.forceChallenge(challengeResponse);
            return false;
        }
        return true;
    }


    public boolean validateUserAndPassword(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData)  {
        UserModel user = getUser(context, inputData);
        boolean shouldClearUserFromCtxAfterBadPassword = !isUserAlreadySetBeforeUsernamePasswordAuth(context);
        return user != null && validatePassword(context, user, inputData, shouldClearUserFromCtxAfterBadPassword) && validateUser(context, user, inputData);
    }

    public boolean validateUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        UserModel user = getUser(context, inputData);
        return user != null && validateUser(context, user, inputData);
    }

    private UserModel getUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        if (isUserAlreadySetBeforeUsernamePasswordAuth(context)) {
            // Get user from the authentication context in case he was already set before this authenticator
            UserModel user = context.getUser();
            testInvalidUser(context, user);
            return user;
        } else {
            // Normal login. In this case this authenticator is supposed to establish identity of the user from the provided username
            context.clearUser();
            return getUserFromForm(context, inputData);
        }
    }

    private UserModel getUserFromForm(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_USERNAME);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return null;
        }

        // remove leading and trailing whitespace
        username = username.trim();

        context.getEvent().detail(Details.USERNAME, username);
        context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

        UserModel user = null;
        try {
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        } catch (ModelDuplicateException mde) {
            ServicesLogger.LOGGER.modelDuplicateException(mde);

            // Could happen during federation import
            if (mde.getDuplicateFieldName() != null && mde.getDuplicateFieldName().equals(UserModel.EMAIL)) {
                setDuplicateUserChallenge(context, Errors.EMAIL_IN_USE, Messages.EMAIL_EXISTS, AuthenticationFlowError.INVALID_USER);
            } else {
                setDuplicateUserChallenge(context, Errors.USERNAME_IN_USE, Messages.USERNAME_EXISTS, AuthenticationFlowError.INVALID_USER);
            }
            return user;
        }

        testInvalidUser(context, user);
        return user;
    }

    private boolean validateUser(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData) {
        if (!enabledUser(context, user)) {
            return false;
        }
        String rememberMe = inputData.getFirst("rememberMe");
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");
        if (remember) {
            context.getAuthenticationSession().setAuthNote(Details.REMEMBER_ME, "true");
            context.getEvent().detail(Details.REMEMBER_ME, "true");
        } else {
            context.getAuthenticationSession().removeAuthNote(Details.REMEMBER_ME);
        }
        context.setUser(user);
        return true;
    }

    public boolean validatePassword(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData, boolean clearUser) {
        String password = inputData.getFirst(CredentialRepresentation.PASSWORD);
        if (password == null || password.isEmpty()) {
            return badPasswordHandler(context, user, clearUser,true);
        }

        if (isDisabledByBruteForce(context, user)) return false;

        if (password != null && !password.isEmpty() && user.credentialManager().isValid(UserCredentialModel.password(password))) {
            return true;
        } else {
            return badPasswordHandler(context, user, clearUser,false);
        }
    }

    // Set up AuthenticationFlowContext error.
    private boolean badPasswordHandler(AuthenticationFlowContext context, UserModel user, boolean clearUser,boolean isEmptyPassword) {
        context.getEvent().user(user);
        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);

        if (isUserAlreadySetBeforeUsernamePasswordAuth(context)) {
            LoginFormsProvider form = context.form();
            form.setAttribute(LoginFormsProvider.USERNAME_HIDDEN, true);
            form.setAttribute(LoginFormsProvider.REGISTRATION_DISABLED, true);
        }

        Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_PASSWORD);
        if(isEmptyPassword) {
            context.forceChallenge(challengeResponse);
        }else{
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        }

        if (clearUser) {
            context.clearUser();
        }
        return false;
    }

    protected boolean isDisabledByBruteForce(AuthenticationFlowContext context, UserModel user) {
        String bruteForceError = getDisabledByBruteForceEventError(context, user);
        if (bruteForceError != null) {
            context.getEvent().user(user);
            context.getEvent().error(bruteForceError);
            Response challengeResponse = challenge(context, disabledByBruteForceError(), disabledByBruteForceFieldError());
            context.forceChallenge(challengeResponse);
            return true;
        }
        return false;
    }

    protected String getDefaultChallengeMessage(AuthenticationFlowContext context) {
        if (isUserAlreadySetBeforeUsernamePasswordAuth(context)) {
            return Messages.INVALID_PASSWORD;
        } else {
            return Messages.INVALID_USER;
        }
    }

    protected boolean isUserAlreadySetBeforeUsernamePasswordAuth(AuthenticationFlowContext context) {
        String userSet = context.getAuthenticationSession().getAuthNote(USER_SET_BEFORE_USERNAME_PASSWORD_AUTH);
        return Boolean.parseBoolean(userSet);
    }
}
