package org.keycloak.authentication.authenticators.resetcred;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResetPassword extends AbstractSetRequiredActionAuthenticator {

    public static final String PROVIDER_ID = "reset-password";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getExecution().isRequired() ||
                (context.getExecution().isOptional() &&
                        configuredFor(context))) {
            context.getClientSession().addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }
        context.success();
    }

    protected boolean configuredFor(AuthenticationFlowContext context) {
        return context.getSession().users().configuredForCredentialType(UserCredentialModel.PASSWORD, context.getRealm(), context.getUser());
    }

    @Override
    public String getDisplayType() {
        return "Reset Password";
    }

    @Override
    public String getHelpText() {
        return "Sets the Update Password required action if execution is REQUIRED.  Will also set it if execution is OPTIONAL and the password is currently configured for it.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
