package org.keycloak.authentication.authenticators.browser;

import java.util.Optional;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.RecoveryAuthnCodesCredentialProvider;
import org.keycloak.credential.RecoveryAuthnCodesCredentialProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.storage.ReadOnlyException;

import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

public class RecoveryAuthnCodesFormAuthenticator implements Authenticator, CredentialValidator<RecoveryAuthnCodesCredentialProvider> {

    public static final String GENERATED_RECOVERY_AUTHN_CODES_NOTE = "RecoveryAuthnCodes.generatedRecoveryAuthnCodes";
    public static final String GENERATED_AT_NOTE = "RecoveryAuthnCodes.generatedAt";

    public RecoveryAuthnCodesFormAuthenticator(KeycloakSession keycloakSession) {
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(createLoginForm(context, false, null, null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.getEvent().detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE)
                .user(context.getUser());
        if (isRecoveryAuthnCodeInputValid(context)) {
            context.success(RecoveryAuthnCodesCredentialModel.TYPE);
        }
    }

    private boolean isRecoveryAuthnCodeInputValid(AuthenticationFlowContext authnFlowContext) {
        boolean result = false;
        MultivaluedMap<String, String> formParamsMap = authnFlowContext.getHttpRequest().getDecodedFormParameters();
        String recoveryAuthnCodeUserInput = formParamsMap.getFirst(RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW);

        UserModel authenticatedUser = authnFlowContext.getUser();
        boolean disabledByBruteForce = isDisabledByBruteForce(authnFlowContext, authenticatedUser);
        if (ObjectUtil.isBlank(recoveryAuthnCodeUserInput)
                || "true".equals(authnFlowContext.getAuthenticationSession().getAuthNote(AbstractUsernameFormAuthenticator.SESSION_INVALID))) {
            // the brute force lock might be lifted in the meantime -> we need to clear the auth session note
            if (!disabledByBruteForce) {
                authnFlowContext.getAuthenticationSession().removeAuthNote(AbstractUsernameFormAuthenticator.SESSION_INVALID);
            } else {
                authnFlowContext.forceChallenge(createLoginForm(authnFlowContext, true,
                        RecoveryAuthnCodesUtils.RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE,
                        RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW));
                return result;
            }
        }

        if (!disabledByBruteForce) {
            boolean isValid = authenticatedUser.credentialManager().isValid(
                    UserCredentialModel.buildFromBackupAuthnCode(recoveryAuthnCodeUserInput.replace("-", "")));
            if (!isValid) {
                Response responseChallenge = createLoginForm(authnFlowContext, true,
                        RecoveryAuthnCodesUtils.RECOVERY_AUTHN_CODES_INPUT_DEFAULT_ERROR_MESSAGE,
                        RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_BROWSER_FLOW);
                authnFlowContext.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, responseChallenge);
            } else {
                result = true;
                Optional<CredentialModel> optUserCredentialFound = RecoveryAuthnCodesUtils.getCredential(authenticatedUser);
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
                    addRequiredAction(authnFlowContext);
                }
            }
        }
        else {
            authnFlowContext.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.SESSION_INVALID, "true");
        }
        return result;
    }

    protected void addRequiredAction(AuthenticationFlowContext authnFlowContext) {
        try {
            authnFlowContext.getUser().addRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES);
        } catch (ReadOnlyException e) {
            // user is read-only, at least add the action to the auth session
            authnFlowContext.getAuthenticationSession().addRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES);
        }
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
        if (!authenticationSession.getRequiredActions().contains(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name())) {
            authenticationSession.addRequiredAction(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name());
        }
    }

    @Override
    public void close() {
    }

    @Override
    public RecoveryAuthnCodesCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (RecoveryAuthnCodesCredentialProvider)session.getProvider(CredentialProvider.class, RecoveryAuthnCodesCredentialProviderFactory.PROVIDER_ID);
    }

    @Override
    public String getType(KeycloakSession session) {
        return RecoveryAuthnCodesCredentialModel.TYPE;
    }
}
