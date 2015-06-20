package org.keycloak.authentication;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderFactory;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public interface AuthenticatorFactory extends ProviderFactory<Authenticator>, ConfiguredProvider {
    Authenticator create();
    String getDisplayType();

    /**
     * General authenticator type, i.e. totp, password, cert.
     *
     * @return null if not a referencable type
     */
    String getReferenceType();

    boolean isConfigurable();

    /**
     * What requirement settings are allowed.
     *
     * @return
     */
    AuthenticationExecutionModel.Requirement[] getRequirementChoices();

}
