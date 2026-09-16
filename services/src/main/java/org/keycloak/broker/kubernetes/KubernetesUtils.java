package org.keycloak.broker.kubernetes;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.keycloak.util.Strings;

import org.apache.commons.io.FileUtils;

import static org.keycloak.broker.kubernetes.KubernetesConstants.KUBERNETES_SERVICE_HOST_KEY;
import static org.keycloak.broker.kubernetes.KubernetesConstants.KUBERNETES_SERVICE_PORT_HTTPS_KEY;
import static org.keycloak.broker.kubernetes.KubernetesConstants.KUBERNETES_SERVICE_PORT_KEY;
import static org.keycloak.broker.kubernetes.KubernetesConstants.SERVICE_ACCOUNT_TOKEN_PATH;
import static org.keycloak.broker.kubernetes.KubernetesConstants.SERVICE_ACCOUNT_TOKEN_PATH_PROPERTY;

final class KubernetesUtils {

    private static final String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";

    private KubernetesUtils() {
    }

    static String discoveryUrl(String issuer) {
        int end = issuer.length();
        while (end > 0 && issuer.charAt(end - 1) == '/') {
            end--;
        }
        String normalizedIssuer = issuer.substring(0, end);
        return normalizedIssuer.endsWith(OIDC_DISCOVERY_PATH) ? normalizedIssuer : normalizedIssuer + OIDC_DISCOVERY_PATH;
    }

    static String getServiceAccountToken() throws Exception {
        File file = new File(System.getProperty(SERVICE_ACCOUNT_TOKEN_PATH_PROPERTY, SERVICE_ACCOUNT_TOKEN_PATH));
        if (!file.exists()) {
            return null;
        }

        String token = FileUtils.readFileToString(file, StandardCharsets.UTF_8).strip();
        return token.isEmpty() ? null : token;
    }

    static boolean isTrustedKubernetesApiUrl(String url) {
        return isTrustedKubernetesApiUrl(url,
                System.getenv(KUBERNETES_SERVICE_HOST_KEY),
                System.getenv(KUBERNETES_SERVICE_PORT_HTTPS_KEY),
                System.getenv(KUBERNETES_SERVICE_PORT_KEY));
    }

    static boolean isTrustedKubernetesApiDiscoveryUrl(String url) {
        if (!isTrustedKubernetesApiUrl(url)) {
            return false;
        }

        try {
            URI discoveryUri = URI.create(discoveryUrl(url));
            return discoveryUri.getPath() != null
                    && discoveryUri.getPath().endsWith(OIDC_DISCOVERY_PATH)
                    && discoveryUri.getQuery() == null
                    && discoveryUri.getFragment() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean isTrustedKubernetesApiUrl(String url, String serviceHost, String httpsServicePort, String servicePort) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }

        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        if ("kubernetes".equalsIgnoreCase(host) || "kubernetes.default".equalsIgnoreCase(host) || "kubernetes.default.svc".equalsIgnoreCase(host) || "kubernetes.default.svc.cluster.local".equalsIgnoreCase(host)) {
            return isTrustedKubernetesApiPort(uri, httpsServicePort, servicePort);
        }

        String normalizedHost = host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
        if (!normalizedHost.equalsIgnoreCase(serviceHost)) {
            return false;
        }

        return isTrustedKubernetesApiPort(uri, httpsServicePort, servicePort);
    }

    static boolean isTrustedKubernetesApiJwksUrl(String jwksUrl, String issuer) {
        URI jwksUri;
        try {
            jwksUri = URI.create(jwksUrl);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!"https".equalsIgnoreCase(jwksUri.getScheme())
                || !"/openid/v1/jwks".equals(jwksUri.getPath())
                || jwksUri.getQuery() != null
                || jwksUri.getFragment() != null) {
            return false;
        }

        if (isTrustedKubernetesApiUrl(jwksUrl)) {
            return true;
        }

        if (!isTrustedKubernetesApiUrl(issuer)) {
            return false;
        }

        return isIpLiteral(jwksUri.getHost())
                // Kubernetes API servers commonly advertise their secure port as 6443 or 8443.
                && (jwksUri.getPort() == 6443 || jwksUri.getPort() == 8443 || isTrustedKubernetesApiPort(jwksUri));
    }

    private static boolean isIpLiteral(String host) {
        if (host == null) {
            return false;
        }

        String normalizedHost = host;
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
        }

        if (normalizedHost.contains(":")) {
            return true;
        }

        String[] octets = normalizedHost.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }

        for (String octet : octets) {
            if (octet.isEmpty() || !octet.chars().allMatch(Character::isDigit)) {
                return false;
            }
            try {
                if (Integer.parseInt(octet) > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    private static boolean isTrustedKubernetesApiPort(URI uri) {
        return isTrustedKubernetesApiPort(uri,
                System.getenv(KUBERNETES_SERVICE_PORT_HTTPS_KEY),
                System.getenv(KUBERNETES_SERVICE_PORT_KEY));
    }

    private static boolean isTrustedKubernetesApiPort(URI uri, String httpsServicePort, String servicePort) {
        String configuredPort = httpsServicePort;
        if (Strings.isEmpty(configuredPort)) {
            configuredPort = servicePort;
        }

        int port = uri.getPort();
        if (port == -1) {
            return Strings.isEmpty(configuredPort) || "443".equals(configuredPort);
        }

        if (Strings.isEmpty(configuredPort)) {
            return port == 443;
        }

        return configuredPort.equals(Integer.toString(port));
    }
}
