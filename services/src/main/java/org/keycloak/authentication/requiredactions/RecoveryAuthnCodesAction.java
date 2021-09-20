package org.keycloak.authentication.requiredactions;

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
        Response challenge = context.form()
                                    .createResponse(UserModel.RequiredAction.CONFIGURE_RECOVERY_AUTHN_CODES);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext reqActionContext) {
        CredentialProvider recvryCodeCredentialProvider;
        RecoveryAuthnCodesCredentialModel recvryCodeCredentialModel;
        MultivaluedMap<String,String> httpReqParamsMap;
        String[] generatedCodesFromFormArray;
        Long generatedAtTime;
        String generatedUserLabel;

        recvryCodeCredentialProvider = reqActionContext.getSession()
                                                       .getProvider(CredentialProvider.class,
                                                                    RecoveryAuthnCodesCredentialProviderFactory.PROVIDER_ID);

        reqActionContext.getEvent().detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE);

        httpReqParamsMap = reqActionContext.getHttpRequest().getDecodedFormParameters();
        // TODO Validation iatanaso
        generatedCodesFromFormArray = httpReqParamsMap.getFirst(RecoveryAuthnCodesUtils.FIELD_GENERATED_RECOVERY_AUTHN_CODES_HIDDEN)
                                                      .split(",");
        // TODO Validation iatanaso
        generatedAtTime = Long.parseLong(httpReqParamsMap.getFirst(RecoveryAuthnCodesUtils.FIELD_GENERATED_AT_HIDDEN));
        // TODO Validation iatanaso
        generatedUserLabel = httpReqParamsMap.getFirst(RecoveryAuthnCodesUtils.FIELD_USER_LABEL_HIDDEN);

        recvryCodeCredentialModel = RecoveryAuthnCodesCredentialModel.createFromValues(generatedCodesFromFormArray,
                                                                                       generatedAtTime,
                                                                                       generatedUserLabel);

        recvryCodeCredentialProvider.createCredential(reqActionContext.getRealm(),
                                                      reqActionContext.getUser(),
                                                      recvryCodeCredentialModel);

        reqActionContext.success();
    }

    @Override
    public void close() {
    }

}
