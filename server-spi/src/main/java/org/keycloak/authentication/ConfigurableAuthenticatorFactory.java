package org.keycloak.authentication;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.provider.ConfiguredProvider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ConfigurableAuthenticatorFactory extends ConfiguredProvider {
    /**
     * Friendly name for the authenticator
     *
     * @return
     */
    String getDisplayType();

    /**
     * General authenticator type, i.e. totp, password, cert.
     *
     * @return null if not a referencable category
     */
    String getReferenceCategory();

    /**
     * Is this authenticator configurable?
     *
     * @return
     */
    boolean isConfigurable();

    /**
     * What requirement settings are allowed.
     *
     * @return
     */
    AuthenticationExecutionModel.Requirement[] getRequirementChoices();

    /**
     *
     * Does this authenticator have required actions that can set if the user does not have
     * this authenticator set up?
     *
     *
     * @return
     */
    boolean isUserSetupAllowed();
}
