package org.keycloak.authentication.authenticators.directgrant;

import org.apache.commons.lang.StringUtils;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.credential.RecoveryAuthnCodesCredentialProvider;
import org.keycloak.credential.RecoveryAuthnCodesCredentialProviderFactory;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.Errors;
import org.keycloak.models.*;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

public class ValidateRecoveryAuthnCode extends AbstractDirectGrantAuthenticator implements CredentialValidator<RecoveryAuthnCodesCredentialProvider> {

    public static final String PROVIDER_ID = "direct-grant-valid-recvry-authn-code";
    public static final String DISPLAY_TYPE = "Recovery Authentication Code Validator for DirectGrant Flow";
    public static final String HELP_TEXT = "Validates the Recovery Authentication Code supplied as a '" +
                                           RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_DIRECT_GRANT_FLOW +
                                           "' form parameter in direct grant request";


    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return DISPLAY_TYPE;
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList<>();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realm, UserModel user) {
        return getCredentialProvider(keycloakSession).isConfiguredFor(realm, user, RecoveryAuthnCodesCredentialModel.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }


    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }


    public RecoveryAuthnCodesCredentialProvider getCredentialProvider(KeycloakSession keycloakSession) {
        return (RecoveryAuthnCodesCredentialProvider)
                keycloakSession.getProvider(CredentialProvider.class,
                                            RecoveryAuthnCodesCredentialProviderFactory.PROVIDER_ID);
    }

    @Override
    public void authenticate(AuthenticationFlowContext authnFlowContext) {
        MultivaluedMap<String, String> httpReqParamsMap;
        String backupCodeInputValue;
        String credentialId;
        Response challengeResponse;
        RecoveryAuthnCodesCredentialProvider recoveryAuthnCodesCredentialProvider;
        boolean isBackupCodeInputValid;
        boolean shouldRaiseFailure = false;

        if (!configuredFor(authnFlowContext.getSession(), authnFlowContext.getRealm(), authnFlowContext.getUser())) {

            if (authnFlowContext.getExecution().isConditional()) {

                authnFlowContext.attempted();

            } else if (authnFlowContext.getExecution().isRequired()) {

                shouldRaiseFailure = true;

            }

        } else {

            recoveryAuthnCodesCredentialProvider = getCredentialProvider(authnFlowContext.getSession());

            httpReqParamsMap = authnFlowContext.getHttpRequest().getDecodedFormParameters();

            backupCodeInputValue = httpReqParamsMap.getFirst(RecoveryAuthnCodesUtils.FIELD_RECOVERY_CODE_IN_DIRECT_GRANT_FLOW);

            if (StringUtils.isEmpty(backupCodeInputValue)) {

                if (authnFlowContext.getUser() != null) {

                    authnFlowContext.getEvent().user(authnFlowContext.getUser());

                }

                shouldRaiseFailure = true;

            } else {

                credentialId = recoveryAuthnCodesCredentialProvider.getDefaultCredential(authnFlowContext.getSession(),
                                                                                         authnFlowContext.getRealm(),
                                                                                         authnFlowContext.getUser())
                                                                   .getId();

                isBackupCodeInputValid = recoveryAuthnCodesCredentialProvider.isValid(authnFlowContext.getRealm(),
                                                                                    authnFlowContext.getUser(),
                                                                                    UserCredentialModel.buildFromBackupAuthnCode(credentialId,
                                                                                                                                 backupCodeInputValue));

                if (!isBackupCodeInputValid) {

                    shouldRaiseFailure = true;

                } else {

                    authnFlowContext.success();

                }

            }
        }

        if (shouldRaiseFailure) {

            authnFlowContext.getEvent().user(authnFlowContext.getUser());
            authnFlowContext.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(),
                                        "invalid_grant",
                                "Invalid User Credentials");
            authnFlowContext.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);

        }
    }

}
