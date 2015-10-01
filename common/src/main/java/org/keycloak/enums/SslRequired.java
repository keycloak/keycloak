package org.keycloak.enums;

import org.keycloak.ClientConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum SslRequired {

    ALL,
    EXTERNAL,
    NONE;

    public boolean isRequired(ClientConnection connection) {
        return isRequired(connection.getRemoteAddr());
    }

    public boolean isRequired(String address) {
        switch (this) {
            case ALL:
                return true;
            case NONE:
                return false;
            case EXTERNAL:
                return !isLocal(address);
            default:
                return true;
        }
    }

    private boolean isLocal(String remoteAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(remoteAddress);
            return inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

}
