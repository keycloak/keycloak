package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.BackupCodeCredentialModel;
import org.keycloak.models.utils.FormMessage;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Optional;

public class BackupCodeFormAuthenticator implements Authenticator {

    public BackupCodeFormAuthenticator() {
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(loginForm(context, false));
    }

    @Override
    public void action(AuthenticationFlowContext context) {

        context.getEvent().detail(Details.CREDENTIAL_TYPE, BackupCodeCredentialModel.TYPE);

        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        String backupCode = params.getFirst("backupCode");

        if (ObjectUtil.isBlank(backupCode)) {
            context.forceChallenge(loginForm(context, true));
            return;
        }

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();

        boolean isValid = credentialManager(context).isValid(realm, user, UserCredentialModel.backupCode(backupCode));

        if (!isValid) {
            Response challenge = loginForm(context, true);

            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        Optional<CredentialModel> credential = credentialManager(context).getStoredCredentialsByTypeStream(realm, user, BackupCodeCredentialModel.TYPE).findFirst();

        if (credential.isPresent()) {
            BackupCodeCredentialModel backupCodeCredentialModel = BackupCodeCredentialModel.createFromCredentialModel(credential.get());

            if (backupCodeCredentialModel.allCodesUsed()) {
                credentialManager(context).removeStoredCredential(realm, user, backupCodeCredentialModel.getId());
                user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_BACKUP_CODES);
            }
        }
        else {
            user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_BACKUP_CODES);
        }

        context.success();
    }

    private static Response loginForm(AuthenticationFlowContext context, boolean withError) {
        LoginFormsProvider form = context.form();

        if (withError) {
            context.getEvent().user(context.getUser());
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);

            form.addError(new FormMessage("backupCode", "backup-codes-error-invalid"));
        }

        return form.createLoginBackupCode();
    }

    private static UserCredentialManager credentialManager(AuthenticationFlowContext context) {
        return context.getSession().userCredentialManager();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, BackupCodeCredentialModel.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_BACKUP_CODES.name());
    }

    @Override
    public void close() {
    }

}
