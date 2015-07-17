package org.keycloak.provider;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ConfiguredProvider {
    String getHelpText();

    List<ProviderConfigProperty> getConfigProperties();
}
