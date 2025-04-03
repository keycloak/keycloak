package org.keycloak.authentication.requiredactions;

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.browser.RecoveryAuthnCodesFormAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.utils.CredentialHelper.createRecoveryCodesCredential;

public class RecoveryAuthnCodesAction implements RequiredActionProvider, RequiredActionFactory, EnvironmentDependentProviderFactory, CredentialRegistrator {

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
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession) {
        return RecoveryAuthnCodesCredentialModel.TYPE;
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
        EventBuilder event = reqActionContext.getEvent();
        event.event(EventType.UPDATE_CREDENTIAL);
        MultivaluedMap<String, String> httpReqParamsMap;

        event.detail(Details.CREDENTIAL_TYPE, RecoveryAuthnCodesCredentialModel.TYPE);

        httpReqParamsMap = reqActionContext.getHttpRequest().getDecodedFormParameters();
        final String generatedCodesString = httpReqParamsMap.getFirst(FIELD_GENERATED_RECOVERY_AUTHN_CODES_HIDDEN);
        final String generatedAtTimeString = httpReqParamsMap.getFirst(FIELD_GENERATED_AT_HIDDEN);
        final String generatedUserLabel = httpReqParamsMap.getFirst(FIELD_USER_LABEL_HIDDEN);

        if (!generatedAtTimeString.equals(reqActionContext.getAuthenticationSession().getAuthNote(RecoveryAuthnCodesFormAuthenticator.GENERATED_AT_NOTE))
                || !generatedCodesString.equals(reqActionContext.getAuthenticationSession().getAuthNote(RecoveryAuthnCodesFormAuthenticator.GENERATED_RECOVERY_AUTHN_CODES_NOTE))) {
            // authn codes have been tampered, sent them again
            requiredActionChallenge(reqActionContext);
            return;
        }

        reqActionContext.getAuthenticationSession().removeAuthNote(RecoveryAuthnCodesFormAuthenticator.GENERATED_AT_NOTE);
        reqActionContext.getAuthenticationSession().removeAuthNote(RecoveryAuthnCodesFormAuthenticator.GENERATED_RECOVERY_AUTHN_CODES_NOTE);

        List<String> generatedCodes = Arrays.asList(generatedCodesString.split(","));
        RecoveryAuthnCodesCredentialModel credentialModel = createFromValues(generatedCodes, Long.valueOf(generatedAtTimeString), generatedUserLabel);

        if ("on".equals(httpReqParamsMap.getFirst("logout-sessions"))) {
            AuthenticatorUtil.logoutOtherSessions(reqActionContext);
        }

        createRecoveryCodesCredential(reqActionContext.getSession(), reqActionContext.getRealm(), reqActionContext.getUser(), credentialModel, generatedCodes);

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
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RECOVERY_CODES);
    }
}
