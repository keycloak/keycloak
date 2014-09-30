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

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }
    }

}
