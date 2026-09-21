package org.keycloak.services.resteasy;

import jakarta.servlet.http.HttpServletRequest;

import org.keycloak.common.ClientConnection;

public class ClientConnectionImpl implements ClientConnection {

    private HttpServletRequest request;

    public ClientConnectionImpl(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String getRemoteAddr() {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null) {
            return forwardedFor;
        }

        return request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return request.getRemoteHost();
    }

    @Override
    public int getRemotePort() {
        return request.getRemotePort();
    }

    @Override
    public String getLocalAddr() {
        return request.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return request.getLocalPort();
    }

}
