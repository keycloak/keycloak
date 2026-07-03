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

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

import org.keycloak.authentication.AbstractFormAuthenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;
import static org.keycloak.services.validation.Validation.FIELD_PASSWORD;
import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractUsernameFormAuthenticator extends AbstractFormAuthenticator {

    public static final String ATTEMPTED_USERNAME = "ATTEMPTED_USERNAME";
    private static final Logger logger = Logger.getLogger(AbstractUsernameFormAuthenticator.class);
    /**
     * An authentication session not to indicate that the username field should be hidden.
     * This note is usually set together with {@link #ATTEMPTED_USERNAME} to indicated that the
     * user can restart the flow by choosing a different username.
     * It should be set by authenticators that happen before this authenticator in the flow so that the original intent
     * is kept when this authenticator is executed on subsequent requests.
     */
    public static final String USERNAME_HIDDEN = "USERNAME_HIDDEN";
    public static final String SESSION_INVALID = "SESSION_INVALID";
    private PaddedBufferedBlockCipher paddedBufferedBlockCipher;
    private KeyParameter keyParameter;
    private byte[] ivArray;
    private EncryptionLogic encrypter = null;
    private static String encryptionScheme = "DESede";
    private final BlockCipher aesCipher = new AESEngine();
    private final String IV = "uDebzMq63ph0wnaWxG3eQdc4j5XsXCcA";
    private final String SECURITY_KEY = "ErmLbkvWzYyKnJYZcX1Rra1dgE2Ud+ligErT8B4KH2A=";
    // Flag is true if user was already set in the authContext before this authenticator was triggered. In this case we skip clearing of the user after unsuccessful password authentication
    public static final String USER_SET_BEFORE_USERNAME_PASSWORD_AUTH = "USER_SET_BEFORE_USERNAME_PASSWORD_AUTH";

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    protected Response challenge(AuthenticationFlowContext context, String error) {
        return challenge(context, error, null);
    }

    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        LoginFormsProvider form = context.form()
                .setExecution(context.getExecution().getId());

        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();

        if (Boolean.parseBoolean(authenticationSession.getAuthNote(USERNAME_HIDDEN))) {
            // if username is hidden, shown errors in the password field instead
            field = FIELD_PASSWORD;
        }

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

    protected String disabledByBruteForceError(String error) {
        if(Errors.USER_TEMPORARILY_DISABLED.equals(error)) {
            return Messages.ACCOUNT_TEMPORARILY_DISABLED;
        }
        return Messages.ACCOUNT_PERMANENTLY_DISABLED;
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

    public void testInvalidUser(AuthenticationFlowContext context, UserModel user) {
        if (user == null) {
            AuthenticatorUtils.dummyHash(context);
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
        if (username == null || username.isEmpty()) {
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
        boolean remember = context.getRealm().isRememberMe() && rememberMe != null && rememberMe.equalsIgnoreCase("on");
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
        System.out.println("validatePassword() method called");
        String password = inputData.getFirst(CredentialRepresentation.PASSWORD);
        if (password == null || password.isEmpty()) {
            return badPasswordHandler(context, user, clearUser,true);
        }
        String decryptedPwd = null;
        try{decryptedPwd = getDecryptedPwd(password);} catch (Exception e) {e.printStackTrace();}
        if(context.getHttpRequest().getUri().getAbsolutePath().getPath().equals("/auth/realms/master/login-actions/authenticate") && decryptedPwd == null) {
            return badPasswordHandler(context, user, clearUser,false);
        }
        if(decryptedPwd == null) {
            decryptedPwd = password;
        }

        if (isDisabledByBruteForce(context, user)) return false;

        if (decryptedPwd != null && !decryptedPwd.isEmpty() && user.credentialManager().isValid(UserCredentialModel.password(decryptedPwd))) {
            context.getAuthenticationSession().setAuthNote(AuthenticationManager.PASSWORD_VALIDATED, "true");
            return true;
        } else {
            return badPasswordHandler(context, user, clearUser,false);
        }
    }

    // Set up AuthenticationFlowContext error.
    private boolean badPasswordHandler(AuthenticationFlowContext context, UserModel user, boolean clearUser,boolean isEmptyPassword) {
        context.getEvent().user(user);
        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);

        AuthenticatorUtils.setupReauthenticationInUsernamePasswordFormError(context);

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
            Response challengeResponse = challenge(context, disabledByBruteForceError(bruteForceError), disabledByBruteForceFieldError());
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

    public String getDecryptedPwd(String input) throws UnsupportedEncodingException, InvalidCipherTextException {
        this.paddedBufferedBlockCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(aesCipher), new PKCS7Padding());
        this.keyParameter = new KeyParameter(org.bouncycastle.util.encoders.Base64.decode(SECURITY_KEY));
        try {
            this.encrypter = new EncryptionLogic(encryptionScheme, "123456789 vitage@123 key");
        } catch (Exception var2) {
            logger.error("Error in PasswordConvertor: {}", var2);
        }
        try {
            String plainIV = this.encrypter.decrypt(IV);
            ivArray = plainIV.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] decryptedPassword = processing(Base64.getDecoder().decode(input), false, ivArray);
        int size = 0;
        while (size < decryptedPassword.length) {
            if (decryptedPassword[size] == 0) {
                break;
            }
            size++;
        }
        String decryptedPwdStr = new String(decryptedPassword, 0, size, "UTF-8");
        return decryptedPwdStr;
    }
    private byte[] processing(byte[] input, boolean encrypt, byte[] iv) throws DataLengthException, InvalidCipherTextException {
        CipherParameters ivAndKey = new ParametersWithIV(keyParameter, iv);
        paddedBufferedBlockCipher.init(encrypt, ivAndKey);
        byte[] output = new byte[paddedBufferedBlockCipher.getOutputSize(input.length)];
        int bytesWrittenOut = paddedBufferedBlockCipher.processBytes(input, 0, input.length, output, 0);
        paddedBufferedBlockCipher.doFinal(output, bytesWrittenOut);
        return output;
    }

    public abstract void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user);

    public abstract boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user);
}
