package org.keycloak.authentication.requiredactions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.Profile;
import org.keycloak.credential.RecoveryAuthnCodesCredentialProviderFactory;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.Details;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class RecoveryAuthnCodesAction implements RequiredActionProvider, RequiredActionFactory, EnvironmentDependentProviderFactory {

    private static final String FIELD_GENERATED_RECOVERY_AUTHN_CODES_HIDDEN = "generatedRecoveryAuthnCodes";
    private static final String FIELD_GENERATED_AT_HIDDEN = "generatedAt";
    private static final String FIELD_USER_LABEL_HIDDEN = "userLabel";
    public static final String PROVIDER_ID = UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES.name();
    private static final RecoveryAuthnCodesAction INSTANCE = new RecoveryAuthnCodesAction();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Recovery Authentication Codes";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form().createResponse(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext reqActionContext) {
        CredentialProvider recoveryCodeCredentialProvider;
        MultivaluedMap<String, String> httpReqParamsMap;
        Long generatedAtTime;
        String generatedUserLabel;

        recoveryCodeCredentialProvider = reqActionContext.getSession().getProvider(CredentialProvider.class,
                RecoveryAuthnCodesCredentialProviderFactory.PROVIDER_ID);

        reqActionContext.getEvent().detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE);

        httpReqParamsMap = reqActionContext.getHttpRequest().getDecodedFormParameters();
        List<String> generatedCodes = new ArrayList<>(
                Arrays.asList(httpReqParamsMap.getFirst(FIELD_GENERATED_RECOVERY_AUTHN_CODES_HIDDEN).split(",")));
        generatedAtTime = Long.parseLong(httpReqParamsMap.getFirst(FIELD_GENERATED_AT_HIDDEN));
        generatedUserLabel = httpReqParamsMap.getFirst(FIELD_USER_LABEL_HIDDEN);

        RecoveryAuthnCodesCredentialModel credentialModel = createFromValues(generatedCodes, generatedAtTime, generatedUserLabel);

        recoveryCodeCredentialProvider.createCredential(reqActionContext.getRealm(), reqActionContext.getUser(),
                credentialModel);

        reqActionContext.success();
    }

    protected RecoveryAuthnCodesCredentialModel createFromValues(List<String> generatedCodes, Long generatedAtTime, String generatedUserLabel) {
        return RecoveryAuthnCodesCredentialModel.createFromValues(generatedCodes,
                generatedAtTime, generatedUserLabel);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.RECOVERY_CODES);
    }
}
