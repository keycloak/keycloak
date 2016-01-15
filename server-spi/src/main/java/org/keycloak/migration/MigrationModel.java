package org.keycloak.migration;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface MigrationModel {
    /**
     * Must have the form of major.minor.micro as the version is parsed and numbers are compared
     */
    String LATEST_VERSION = "1.8.0";

    String getStoredVersion();
    void setStoredVersion(String version);
}
