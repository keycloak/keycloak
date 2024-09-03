package org.keycloak.cookie;

import java.net.URI;
import java.util.regex.Pattern;

class SecureContextResolver {

    private static final Pattern LOCALHOST_IPV4 = Pattern.compile("127.\\d{1,3}.\\d{1,3}.\\d{1,3}");

    /**
     * Determines if a URI is potentially trustworthy, meaning a user agent can generally trust it to deliver data securely.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Security/Secure_Contexts">MDN Web Docs — Secure Contexts</a>
     * @see <a href="https://w3c.github.io/webappsec-secure-contexts/#algorithms">W3C Secure Contexts specification — Is origin potentially trustworthy?</a>
     * @param uri The URI to check.
     * @return Whether the URI can be considered potentially trustworthy.
     */
    static boolean isSecureContext(URI uri) {
        if (uri.getScheme().equals("https")) {
            return true;
        }

        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        // The host matches a CIDR notation of ::1/128
        if (host.equals("[::1]") || host.equals("[0000:0000:0000:0000:0000:0000:0000:0001]")) {
            return true;
        }

        // The host matches a CIDR notation of 127.0.0.0/8
        if (LOCALHOST_IPV4.matcher(host).matches()) {
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
