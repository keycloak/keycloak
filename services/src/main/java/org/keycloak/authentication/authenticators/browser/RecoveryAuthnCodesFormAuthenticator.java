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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

public class RecoveryAuthnCodesFormAuthenticator implements Authenticator {

    public RecoveryAuthnCodesFormAuthenticator(KeycloakSession keycloakSession) {
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
        MultivaluedMap<String, String> formParamsMap = authnFlowContext.getHttpRequest().getDecodedFormParameters();
        String recoveryAuthnCodeUserInput = formParamsMap.getFirst(RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW);

        if (ObjectUtil.isBlank(recoveryAuthnCodeUserInput)) {
            authnFlowContext.forceChallenge(createLoginForm(authnFlowContext, true,
                    RecoveryAuthnCodesUtils.RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE,
                    RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW));
            return result;
        }
        RealmModel targetRealm = authnFlowContext.getRealm();
        UserModel authenticatedUser = authnFlowContext.getUser();
        if (!isDisabledByBruteForce(authnFlowContext, authenticatedUser)) {
            boolean isValid = authenticatedUser.credentialManager().isValid(
                    UserCredentialModel.buildFromBackupAuthnCode(recoveryAuthnCodeUserInput.replace("-", "")));
            if (!isValid) {
                Response responseChallenge = createLoginForm(authnFlowContext, true,
                        RecoveryAuthnCodesUtils.RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE,
                        RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW);
                authnFlowContext.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, responseChallenge);
            } else {
                result = true;
                Optional<CredentialModel> optUserCredentialFound = authenticatedUser.credentialManager().getStoredCredentialsByTypeStream(
                        RecoveryAuthnCodesCredentialModel.TYPE).findFirst();
                RecoveryAuthnCodesCredentialModel recoveryCodeCredentialModel = null;
                if (optUserCredentialFound.isPresent()) {
                    recoveryCodeCredentialModel = RecoveryAuthnCodesCredentialModel
                            .createFromCredentialModel(optUserCredentialFound.get());
                    if (recoveryCodeCredentialModel.allCodesUsed()) {
                        authenticatedUser.credentialManager().removeStoredCredentialById(
                                recoveryCodeCredentialModel.getId());
                    }
                }
                if (recoveryCodeCredentialModel == null || recoveryCodeCredentialModel.allCodesUsed()) {
                    authenticatedUser.addRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES);
                }
            }
        }
        return result;
    }

    protected boolean isDisabledByBruteForce(AuthenticationFlowContext authnFlowContext, UserModel authenticatedUser) {
        String bruteForceError;
        Response challengeResponse;
        bruteForceError = getDisabledByBruteForceEventError(authnFlowContext, authenticatedUser);
        if (bruteForceError == null) {
            return false;
        }
        authnFlowContext.getEvent().user(authenticatedUser);
        authnFlowContext.getEvent().error(bruteForceError);
        challengeResponse = createLoginForm(authnFlowContext, false, Messages.INVALID_USER, FIELD_USERNAME);
        authnFlowContext.forceChallenge(challengeResponse);
        return true;
    }

    protected String getDisabledByBruteForceEventError(AuthenticationFlowContext authnFlowContext, UserModel authenticatedUser) {
        return AuthenticatorUtils.getDisabledByBruteForceEventError(authnFlowContext, authenticatedUser);
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
        return user.credentialManager().isConfiguredFor(RecoveryAuthnCodesCredentialModel.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();
        authenticationSession.addRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
    }

    @Override
    public void close() {
    }

}
