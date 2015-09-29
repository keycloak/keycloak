package org.keycloak.services.util;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResolveRelative {
    public static String resolveRelativeUri(URI requestUri, String rootUrl, String url) {
        if (url == null || !url.startsWith("/")) return url;
        if (rootUrl != null) {
            return rootUrl + url;
        } else {
            UriBuilder builder = UriBuilder.fromPath(url).host(requestUri.getHost());
            builder.scheme(requestUri.getScheme());
            if (requestUri.getPort() != -1) {
                builder.port(requestUri.getPort());
            }
            return builder.build().toString();
        }
    }
}
