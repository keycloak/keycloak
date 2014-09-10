package org.keycloak.util;

import java.net.URI;

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

}
