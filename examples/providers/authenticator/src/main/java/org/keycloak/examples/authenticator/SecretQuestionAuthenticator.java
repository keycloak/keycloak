package org.keycloak.examples.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AbstractFormAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.CredentialValidation;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SecretQuestionAuthenticator extends AbstractFormAuthenticator {

    public static final String CREDENTIAL_TYPE = "secret_question";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response challenge = loginForm(context).createForm("secret_question.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String secret = formData.getFirst("secret");
        if (secret == null || secret.trim().equals("")) {
            badSecret(context);
            return;
        }

        UserCredentialValueModel cred = null;
        for (UserCredentialValueModel model : context.getUser().getCredentialsDirectly()) {
            if (model.getType().equals(CREDENTIAL_TYPE)) {
                cred = model;
                break;
            }
        }
        if (cred == null) {
            badSecret(context);
            return;
        }

        boolean validated = CredentialValidation.validateHashedCredential(context.getRealm(), context.getUser(), secret, cred);
        if (!validated) {
            badSecret(context);
            return;
        }

        context.success();
    }

    private void badSecret(AuthenticationFlowContext context) {
        Response challenge =  loginForm(context)
                .setError("badSecret")
                .createForm("secret_question.ftl");
        context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.users().configuredForCredentialType(CREDENTIAL_TYPE, realm, user);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(CREDENTIAL_TYPE + "_CONFIG");
    }
}
