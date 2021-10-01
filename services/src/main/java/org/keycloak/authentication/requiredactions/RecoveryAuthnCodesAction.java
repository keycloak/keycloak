package org.keycloak.authentication.requiredactions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.RecoveryAuthnCodesCredentialProviderFactory;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.Details;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.utils.RecoveryAuthnCodesUtils;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class RecoveryAuthnCodesAction implements RequiredActionProvider, RequiredActionFactory {

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
        // TODO Validation iatanaso
        List<String> generatedCodes = new ArrayList<>(
                Arrays.asList(httpReqParamsMap.getFirst(FIELD_GENERATED_RECOVERY_AUTHN_CODES_HIDDEN).split(",")));
        // TODO Validation iatanaso
        generatedAtTime = Long.parseLong(httpReqParamsMap.getFirst(FIELD_GENERATED_AT_HIDDEN));
        // TODO Validation iatanaso
        generatedUserLabel = httpReqParamsMap.getFirst(FIELD_USER_LABEL_HIDDEN);

        RecoveryAuthnCodesCredentialModel credentialModel = RecoveryAuthnCodesCredentialModel.createFromValues(generatedCodes,
                generatedAtTime, generatedUserLabel);

        recoveryCodeCredentialProvider.createCredential(reqActionContext.getRealm(), reqActionContext.getUser(),
                credentialModel);

        reqActionContext.success();
    }

    @Override
    public void close() {
    }

}
