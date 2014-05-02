package org.keycloak.services.util;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResolveRelative {
    public static String resolveRelativeUri(URI requestUri, String url) {
        if (url == null || !url.startsWith("/")) return url;
        UriBuilder builder = UriBuilder.fromPath(url).host(requestUri.getHost());
        builder.scheme(requestUri.getScheme());
        if (requestUri.getPort() != -1) {
            builder.port(requestUri.getPort());
        }
        return builder.build().toString();
    }
}
