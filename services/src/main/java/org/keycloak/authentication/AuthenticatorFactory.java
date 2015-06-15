package org.keycloak.authentication;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderFactory;

import java.util.List;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public interface AuthenticatorFactory extends ProviderFactory<Authenticator>, ConfiguredProvider {
    Authenticator create(AuthenticatorModel model);
    String getDisplayCategory();
    String getDisplayType();

    /**
     * General authenticator type, i.e. totp, password, cert
     *
     * @return null if not a referencable type
     */
    String getReferenceType();

    boolean isConfigurable();

    /**
     * What requirement settings are allowed.  For example, KERBEROS can only be required because of the way its challenges
     * work.
     *
     * @return
     */
    AuthenticationExecutionModel.Requirement[] getRequirementChoices();

}
