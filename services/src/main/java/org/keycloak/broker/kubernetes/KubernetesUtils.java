package org.keycloak.broker.kubernetes;

import java.io.File;
import java.net.InetAddress;
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

    private KubernetesUtils() {
    }

    static String discoveryUrl(String issuer) {
        int end = issuer.length();
        while (end > 0 && issuer.charAt(end - 1) == '/') {
            end--;
        }
        return issuer.substring(0, end) + "/.well-known/openid-configuration";
    }

    static String getServiceAccountToken() throws Exception {
        File file = new File(System.getProperty(SERVICE_ACCOUNT_TOKEN_PATH_PROPERTY, SERVICE_ACCOUNT_TOKEN_PATH));
        if (!file.exists()) {
            return null;
        }

        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    static boolean isTrustedKubernetesApiUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!"https".equals(uri.getScheme())) {
            return false;
        }

        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        if ("kubernetes".equals(host) || "kubernetes.default".equals(host) || "kubernetes.default.svc".equals(host) || "kubernetes.default.svc.cluster.local".equals(host)) {
            return isTrustedKubernetesApiPort(uri);
        }

        String serviceHost = System.getenv(KUBERNETES_SERVICE_HOST_KEY);
        if (!host.equals(serviceHost)) {
            return false;
        }

        return isTrustedKubernetesApiPort(uri);
    }

    static boolean isTrustedKubernetesApiJwksUrl(String jwksUrl, String issuer) {
        if (isTrustedKubernetesApiUrl(jwksUrl)) {
            return true;
        }

        if (!isTrustedKubernetesApiUrl(issuer)) {
            return false;
        }

        URI jwksUri;
        try {
            jwksUri = URI.create(jwksUrl);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return "https".equals(jwksUri.getScheme())
                && "/openid/v1/jwks".equals(jwksUri.getPath())
                && jwksUri.getQuery() == null
                && jwksUri.getFragment() == null
                && isIpLiteral(jwksUri.getHost());
    }

    private static boolean isIpLiteral(String host) {
        if (host == null || (!host.contains(":") && !host.matches("\\d+(\\.\\d+){3}"))) {
            return false;
        }

        try {
            String address = InetAddress.getByName(host).getHostAddress();
            return host.contains(":") || address.equals(host);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isTrustedKubernetesApiPort(URI uri) {
        String servicePort = System.getenv(KUBERNETES_SERVICE_PORT_HTTPS_KEY);
        if (Strings.isEmpty(servicePort)) {
            servicePort = System.getenv(KUBERNETES_SERVICE_PORT_KEY);
        }

        int port = uri.getPort();
        if (port == -1) {
            return true;
        }

        if (Strings.isEmpty(servicePort)) {
            return port == 443;
        }

        return servicePort.equals(Integer.toString(port));
    }
}
