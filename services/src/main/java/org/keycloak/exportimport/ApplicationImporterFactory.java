package org.keycloak.exportimport;

import org.keycloak.provider.ProviderFactory;

/**
 * Provider plugin interface for importing applications from an arbitrary configuration format
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ApplicationImporterFactory extends ProviderFactory<ApplicationImporter> {
    public String getDisplayName();
}
