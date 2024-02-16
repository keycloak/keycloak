package org.keycloak.testsuite.runonserver;

import org.keycloak.common.Version;

/**
 * Created by st on 26.01.17.
 */
public class ServerVersion implements FetchOnServerWrapper<String> {

    @Override
    public FetchOnServer getRunOnServer() {
        return (FetchOnServer) session -> Version.RESOURCES_VERSION;
    }

    @Override
    public Class<String> getResultClass() {
        return String.class;
    }

}
