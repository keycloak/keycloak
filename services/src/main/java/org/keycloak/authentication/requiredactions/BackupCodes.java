package org.keycloak.authentication.requiredactions;

import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.BackupCodeCredentialProviderFactory;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.BackupCodeCredentialModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class BackupCodes implements RequiredActionProvider {

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
                .createResponse(UserModel.RequiredAction.CONFIGURE_BACKUP_CODES);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // TODO: Hash backupCodes
        context.getEvent().detail(Details.CREDENTIAL_TYPE, BackupCodeCredentialModel.TYPE);

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String[] codes = formData.getFirst("backupCodes").split(",");
        long generatedAt = Long.parseLong(formData.getFirst("generatedAt"));

        BackupCodeCredentialModel credentialModel = BackupCodeCredentialModel.createFromValues(codes, generatedAt, 0, "none");

        CredentialProvider provider = context.getSession().getProvider(CredentialProvider.class, BackupCodeCredentialProviderFactory.PROVIDER_ID);
        provider.createCredential(context.getRealm(), context.getUser(), credentialModel);

        context.success();
    }

    @Override
    public void close() {
    }

}
