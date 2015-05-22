package org.keycloak.authentication.authenticators;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationFlow {

    /**
     * Hardcoded models just to test this stuff.  It is temporary
     */
    static List<AuthenticationExecutionModel> hardcoded = new ArrayList<>();

    /*
    static {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId("1");
        model.setAlias("cookie");
        model.setMasterAuthenticator(true);
        model.setProviderId(CookieAuthenticatorFactory.PROVIDER_ID);
        model.setPriority(0);
        model.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        model.setUserSetupAllowed(false);
        hardcoded.add(model);
        model = new AuthenticatorModel();
        model.setId("2");
        model.setAlias("user form");
        model.setMasterAuthenticator(false);
        model.setProviderId(LoginFormUsernameAuthenticatorFactory.PROVIDER_ID);
        model.setPriority(1);
        model.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        model.setUserSetupAllowed(false);
        hardcoded.add(model);
        model = new AuthenticatorModel();
        model.setId("3");
        model.setAlias("password form");
        model.setMasterAuthenticator(false);
        model.setProviderId(LoginFormUsernameAuthenticatorFactory.PROVIDER_ID);
        model.setPriority(2);
        model.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        model.setUserSetupAllowed(false);
        hardcoded.add(model);
        model = new AuthenticatorModel();
        model.setId("4");
        model.setAlias("otp form");
        model.setMasterAuthenticator(false);
        model.setProviderId(OTPFormAuthenticatorFactory.PROVIDER_ID);
        model.setPriority(3);
        model.setRequirement(AuthenticationExecutionModel.Requirement.OPTIONAL);
        model.setUserSetupAllowed(false);
        hardcoded.add(model);
    }
    */
}
