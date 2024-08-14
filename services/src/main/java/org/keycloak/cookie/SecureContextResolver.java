package org.keycloak.cookie;

import java.net.URI;

class SecureContextResolver {

    static boolean isSecureContext(URI uri) {
        if (uri.getScheme().equals("https")) {
            return true;
        }

        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        if (host.equals("[::1]") || host.equals("[0000:0000:0000:0000:0000:0000:0000:0001]")) {
            return true;
        }

        if (host.matches("127.\\d{1,3}.\\d{1,3}.\\d{1,3}")) {
            return true;
        }

        if (host.equals("localhost") || host.equals("localhost.")) {
            return true;
        }

        if (host.endsWith(".localhost") || host.endsWith(".localhost.")) {
            return true;
        }

        return false;
    }

}
