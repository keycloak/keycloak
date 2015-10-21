package org.keycloak.protocol.oidc.utils;

import org.keycloak.models.ClientModel;
import org.keycloak.common.util.UriUtils;

import javax.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * Created by st on 22.09.15.
 */
public class WebOriginsUtils {

    public static final String INCLUDE_REDIRECTS = "+";

    public static Set<String> resolveValidWebOrigins(UriInfo uriInfo, ClientModel client) {
        Set<String> webOrigins = client.getWebOrigins();
        if (webOrigins != null && webOrigins.contains("+")) {
            webOrigins.remove(INCLUDE_REDIRECTS);
            client.getRedirectUris();
            for (String redirectUri : RedirectUtils.resolveValidRedirects(uriInfo, client.getRootUrl(), client.getRedirectUris())) {
                if (redirectUri.startsWith("http://") || redirectUri.startsWith("https://")) {
                    webOrigins.add(UriUtils.getOrigin(redirectUri));
                }
            }
        }
        return webOrigins;
    }

}
