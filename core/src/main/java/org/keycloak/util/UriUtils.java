package org.keycloak.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UriUtils {

    public static String getOrigin(URI uri) {
        return getOrigin(uri.toString());
    }

    public static String getOrigin(String uri) {
        String u = uri.toString();
        return u.substring(0, u.indexOf('/', 8));
    }

    /**
     * Get origin based on current hostname
     *
     * @param scheme
     * @param port
     * @return Address like "http://myHost:8080"
     */
    public static String getLocalOrigin(String scheme, Integer port) {
        String hostname = getHostName();
        StringBuilder sb = new StringBuilder(scheme + "://" + hostname);
        if (port != null && port != -1) {
            sb.append(":").append(port);
        }
        return sb.toString();
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }
    }

}
