package org.keycloak.util;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UriUtils {

    private static final Pattern originPattern = Pattern.compile("(http://|https://)[\\w-]+(\\.[\\w-]+)*(:[\\d]{2,5})?");

    public static String getOrigin(URI uri) {
        return getOrigin(uri.toString());
    }

    public static String getOrigin(String uri) {
        String u = uri.toString();
        return u.substring(0, u.indexOf('/', 8));
    }

    public static boolean isOrigin(String url) {
        return originPattern.matcher(url).matches();
    }

}
