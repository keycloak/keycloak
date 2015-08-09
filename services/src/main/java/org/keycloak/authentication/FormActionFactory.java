package org.keycloak.authentication;

import org.keycloak.provider.ProviderFactory;

/**
 * Factory for instantiating FormAction objects.  This is a singleton and created when Keycloak boots.
 *
 * You must specify a file
 * META-INF/services/org.keycloak.authentication.FormActionFactory in the jar that this class is contained in
 * This file must have the fully qualified class name of all your FormActionFactory classes
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormActionFactory extends ProviderFactory<FormAction>, ConfigurableAuthenticatorFactory {
}
