package org.keycloak.encoding;

import org.keycloak.models.KeycloakSession;

public class ResourceEncodingHelper {

    public static ResourceEncodingProvider getResourceEncodingProvider(KeycloakSession session, String contentType) {
        String acceptEncoding = session.getContext().getRequestHeaders().getHeaderString("Accept-Encoding");
        if (acceptEncoding != null) {
            for (String e : acceptEncoding.split(",")) {
                e = e.trim();
                ResourceEncodingProviderFactory f = (ResourceEncodingProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(ResourceEncodingProvider.class, e);
                if (f != null && f.encodeContentType(contentType)) {
                    ResourceEncodingProvider provider = session.getProvider(ResourceEncodingProvider.class, e.trim());
                    if (provider != null) {
                        return provider;
                    }
                } else {
                    return null;
                }
            }
        }
        return null;
    }

}
