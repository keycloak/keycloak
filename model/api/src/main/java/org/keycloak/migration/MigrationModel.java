package org.keycloak.migration;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface MigrationModel {
    /**
     * Must have the form of major.minor.micro as the version is parsed and numbers are compared
     */
    public static final String LATEST_VERSION = "1.5.0";

    String getStoredVersion();
    void setStoredVersion(String version);
}
