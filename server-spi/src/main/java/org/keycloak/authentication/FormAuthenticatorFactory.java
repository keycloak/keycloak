package org.keycloak.authentication;

import org.keycloak.provider.ProviderFactory;

/**
 * Factory for instantiating FormAuthenticators.  This is a singleton and created when Keycloak boots.
 *
 * You must specify a file
 * META-INF/services/org.keycloak.authentication.FormAuthenticatorFactory in the jar that this class is contained in
 * This file must have the fully qualified class name of all your FormAuthenticatorFactory classes
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormAuthenticatorFactory extends ProviderFactory<FormAuthenticator>, ConfigurableAuthenticatorFactory {
}
