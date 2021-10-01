package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

public class RecoveryAuthnCodesFormAuthenticator implements Authenticator {

    private final UserCredentialManager userCredentialManager;

    public RecoveryAuthnCodesFormAuthenticator(KeycloakSession keycloakSession) {
        this.userCredentialManager = keycloakSession.userCredentialManager();
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(createLoginForm(context, false, null, null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.getEvent().detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE);
        if (isRecoveryAuthnCodeInputValid(context)) {
            context.success();
        }
    }

    private boolean isRecoveryAuthnCodeInputValid(AuthenticationFlowContext authnFlowContext) {
        boolean result = false;
        MultivaluedMap<String, String> formParamsMap;
        String recoveryAuthnCodeUserInput;
        RealmModel targetRealm;
        UserModel authenticatedUser;
        Response responseChallenge;
        boolean isValid;
        Optional<CredentialModel> optUserCredentialFound;
        RecoveryAuthnCodesCredentialModel recoveryCodeCredentialModel = null;

        formParamsMap = authnFlowContext.getHttpRequest().getDecodedFormParameters();
        recoveryAuthnCodeUserInput = formParamsMap.getFirst(RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW);

        if (ObjectUtil.isBlank(recoveryAuthnCodeUserInput)) {
            authnFlowContext.forceChallenge(createLoginForm(authnFlowContext, true,
                    RecoveryAuthnCodesUtils.RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE,
                    RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW));
        } else {
            targetRealm = authnFlowContext.getRealm();
            authenticatedUser = authnFlowContext.getUser();
            if (!isDisabledByBruteForce(authnFlowContext, authenticatedUser)) {
                isValid = this.userCredentialManager.isValid(targetRealm, authenticatedUser,
                        UserCredentialModel.buildFromBackupAuthnCode(recoveryAuthnCodeUserInput.replace("-", "")));
                if (!isValid) {
                    responseChallenge = createLoginForm(authnFlowContext, true,
                            RecoveryAuthnCodesUtils.RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE,
                            RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW);
                    authnFlowContext.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, responseChallenge);
                } else {
                    result = true;
                    optUserCredentialFound = this.userCredentialManager.getStoredCredentialsByTypeStream(targetRealm,
                            authenticatedUser, RecoveryAuthnCodesCredentialModel.TYPE).findFirst();
                    if (optUserCredentialFound.isPresent()) {
                        recoveryCodeCredentialModel = RecoveryAuthnCodesCredentialModel
                                .createFromCredentialModel(optUserCredentialFound.get());
                        if (recoveryCodeCredentialModel.allCodesUsed()) {
                            this.userCredentialManager.removeStoredCredential(targetRealm, authenticatedUser,
                                    recoveryCodeCredentialModel.getId());
                        }
                    }
                    if (recoveryCodeCredentialModel == null || recoveryCodeCredentialModel.allCodesUsed()) {
                        authenticatedUser.addRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES);
                    }
                }
            }
        }
        return result;
    }

    protected boolean isDisabledByBruteForce(AuthenticationFlowContext authnFlowContext, UserModel authenticatedUser) {
        boolean bolResult = false;
        String bruteForceError;
        Response challengeResponse;
        bruteForceError = AuthenticatorUtils.getDisabledByBruteForceEventError(authnFlowContext.getProtector(),
                authnFlowContext.getSession(), authnFlowContext.getRealm(), authenticatedUser);
        if (bruteForceError != null) {
            authnFlowContext.getEvent().user(authenticatedUser);
            authnFlowContext.getEvent().error(bruteForceError);
            challengeResponse = createLoginForm(authnFlowContext, false, Messages.INVALID_USER, FIELD_USERNAME);
            authnFlowContext.forceChallenge(challengeResponse);
            bolResult = true;
        }
        return bolResult;
    }

    private Response createLoginForm(AuthenticationFlowContext authnFlowContext, boolean withInvalidUserCredentialsError,
            String errorToRaise, String fieldError) {
        Response challengeResponse;
        LoginFormsProvider loginFormsProvider;
        if (withInvalidUserCredentialsError) {
            loginFormsProvider = authnFlowContext.form();
            authnFlowContext.getEvent().user(authnFlowContext.getUser());
            authnFlowContext.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            loginFormsProvider.addError(new FormMessage(fieldError, errorToRaise));
        } else {
            loginFormsProvider = authnFlowContext.form().setExecution(authnFlowContext.getExecution().getId());
            if (errorToRaise != null) {
                if (fieldError != null) {
                    loginFormsProvider.addError(new FormMessage(fieldError, errorToRaise));
                } else {
                    loginFormsProvider.setError(errorToRaise);
                }
            }
        }
        challengeResponse = loginFormsProvider.createLoginRecoveryAuthnCode();
        return challengeResponse;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, RecoveryAuthnCodesCredentialModel.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
    }

    @Override
    public void close() {
    }

}
