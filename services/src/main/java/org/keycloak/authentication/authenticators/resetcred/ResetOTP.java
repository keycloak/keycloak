package org.keycloak.authentication.authenticators.resetcred;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResetOTP extends AbstractSetRequiredActionAuthenticator {

    public static final String PROVIDER_ID = "reset-otp";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getExecution().isRequired() ||
                (context.getExecution().isOptional() &&
                        configuredFor(context))) {
            context.getClientSession().addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        }
        context.success();
    }

    protected boolean configuredFor(AuthenticationFlowContext context) {
        return context.getSession().users().configuredForCredentialType(context.getRealm().getOTPPolicy().getType(), context.getRealm(), context.getUser());
    }

    @Override
    public String getDisplayType() {
        return "Reset OTP";
    }

    @Override
    public String getHelpText() {
        return "Sets the Configure OTP required action if execution is REQUIRED.  Will also set it if execution is OPTIONAL and the OTP is currently configured for it.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
