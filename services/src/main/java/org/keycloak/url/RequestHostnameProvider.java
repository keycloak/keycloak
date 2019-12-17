package org.keycloak.url;

import org.keycloak.urls.HostnameProvider;

import javax.ws.rs.core.UriInfo;

@Deprecated
public class RequestHostnameProvider implements HostnameProvider {

    @Override
    public String getScheme(UriInfo originalUriInfo) {
        return originalUriInfo.getRequestUri().getScheme();
    }

    @Override
    public String getHostname(UriInfo originalUriInfo) {
        return originalUriInfo.getBaseUri().getHost();
    }

    @Override
    public int getPort(UriInfo originalUriInfo) {
        return originalUriInfo.getRequestUri().getPort();
    }

}
