package org.keycloak.scim.resource.spi;

import org.keycloak.provider.ProviderFactory;

public interface ScimResourceTypeProviderFactory<P extends ScimResourceTypeProvider<?>> extends ProviderFactory<P> {

}
