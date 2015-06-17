package org.keycloak.authentication.authenticators;

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.events.Errors;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OTPFormAuthenticator extends AbstractFormAuthenticator implements Authenticator {
    public static final String TOTP_FORM_ACTION = "totp";
    protected AuthenticatorModel model;

    public OTPFormAuthenticator(AuthenticatorModel model) {
        this.model = model;
    }

    @Override
    public void authenticate(AuthenticatorContext context) {
        if (!isAction(context, TOTP_FORM_ACTION)) {
            Response challengeResponse = challenge(context, null);
            context.challenge(challengeResponse);
            return;
        }
        validateOTP(context);
    }

    public void validateOTP(AuthenticatorContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        List<UserCredentialModel> credentials = new LinkedList<>();
        String password = inputData.getFirst(CredentialRepresentation.TOTP);
        if (password == null) {
            Response challengeResponse = challenge(context, null);
            context.challenge(challengeResponse);
            return;
        }
        credentials.add(UserCredentialModel.totp(password));
        boolean valid = context.getSession().users().validCredentials(context.getRealm(), context.getUser(), credentials);
        if (!valid) {
            context.getEvent().user(context.getUser())
                    .error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = challenge(context, Messages.INVALID_TOTP);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_CREDENTIALS, challengeResponse);
            return;
        }
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    protected Response challenge(AuthenticatorContext context, String error) {
        String accessCode = context.generateAccessCode();
        URI action = AbstractFormAuthenticator.getActionUrl(context, accessCode, TOTP_FORM_ACTION);
        LoginFormsProvider forms = context.getSession().getProvider(LoginFormsProvider.class)
                .setActionUri(action)
                .setClientSessionCode(accessCode);
        if (error != null) forms.setError(error);

        return forms.createLoginTotp();
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
