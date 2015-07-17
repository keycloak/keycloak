package org.keycloak.protocol;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ProtocolMapper extends Provider, ProviderFactory<ProtocolMapper>,ConfiguredProvider {
    String getProtocol();
    String getDisplayCategory();
    String getDisplayType();

}
