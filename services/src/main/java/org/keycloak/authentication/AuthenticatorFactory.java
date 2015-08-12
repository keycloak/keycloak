package org.keycloak.authentication;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory for creating Authenticator instances.  This is a singleton and created when Keycloak boots.
 *
 * You must specify a file
 * META-INF/services/org.keycloak.authentication.AuthenticatorFactory in the jar that this class is contained in
 * This file must have the fully qualified class name of all your AuthenticatorFactory classes
 *
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public interface AuthenticatorFactory extends ProviderFactory<Authenticator>, ConfigurableAuthenticatorFactory {
}
