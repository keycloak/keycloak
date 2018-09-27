package org.keycloak.testsuite.forms;

import java.io.Serializable;

/**
 * The only purpose of this class is to serialize data obtained from oauth field
 * and pass it to the server.
 */
public class SerializableApplicationData implements Serializable {

    public final String applicationBaseUrl;
    public final String applicationManagementUrl;
    public final String applicationRedirectUrl;

    public SerializableApplicationData(String applicationBaseUrl, String applicationManagementUrl, String applicationRedirectUrl) {
        this.applicationBaseUrl = applicationBaseUrl;
        this.applicationManagementUrl = applicationManagementUrl;
        this.applicationRedirectUrl = applicationRedirectUrl;
    }
}
