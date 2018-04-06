package org.keycloak.performance;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ClientInfo {

    public final int index;
    public final String clientId;
    public final String secret;
    public final String appUrl;


    public ClientInfo(int index, String clientId, String secret, String appUrl) {
        this.index = index;
        this.clientId = clientId;
        this.secret = secret;
        this.appUrl = appUrl;
    }
}
