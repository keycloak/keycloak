package org.keycloak.authentication.authenticators;

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LoginFormOTPAuthenticator extends LoginFormUsernameAuthenticator {

    public LoginFormOTPAuthenticator(AuthenticatorModel model) {
        super(model);
    }

    @Override
    public void authenticate(AuthenticatorContext context) {
        if (!isAction(context, LOGIN_FORM_ACTION)) {
            context.failure(AuthenticationProcessor.Error.INTERNAL_ERROR);
            return;
        }
        validateOTP(context);
    }

    protected Response badPassword(AuthenticatorContext context) {
        return loginForm(context).setError(Messages.INVALID_USER).createLogin();
    }

    public void validateOTP(AuthenticatorContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getFormParameters();
        List<UserCredentialModel> credentials = new LinkedList<>();
        String password = inputData.getFirst(CredentialRepresentation.TOTP);
        if (password == null) {
            Response challengeResponse = badPassword(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_CREDENTIALS, challengeResponse);
            return;
        }
        credentials.add(UserCredentialModel.totp(password));
        boolean valid = context.getSession().users().validCredentials(context.getRealm(), context.getUser(), credentials);
        if (!valid) {
            Response challengeResponse = badPassword(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_CREDENTIALS, challengeResponse);
            return;
        }
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.users().configuredForCredentialType(UserCredentialModel.TOTP, realm, user) && user.isTotp();
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        if (!user.getRequiredActions().contains(UserModel.RequiredAction.CONFIGURE_TOTP.name())) {
            user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        }

    }

    @Override
    public void close() {

    }
}
