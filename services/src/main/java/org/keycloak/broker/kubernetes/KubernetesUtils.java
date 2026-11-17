package org.keycloak.broker.kubernetes;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.JsonWebToken;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import static org.keycloak.broker.kubernetes.KubernetesConstants.SERVICE_ACCOUNT_TOKEN_PATH;

final class KubernetesUtils {

    private static final String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";
    private static final Logger logger = Logger.getLogger(KubernetesUtils.class);

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

    static String getServiceAccountToken() {
        return getServiceAccountToken(new File(SERVICE_ACCOUNT_TOKEN_PATH));
    }

    static String getServiceAccountToken(File file) {
        try {
            if (!file.exists()) {
                return null;
            }

            String token = FileUtils.readFileToString(file, StandardCharsets.UTF_8).strip();
            return token.isEmpty() ? null : token;
        } catch (Exception e) {
            logger.warn("Failed to read service account token file", e);
            return null;
        }
    }

    static String getServiceAccountToken(String issuer) {
        return getServiceAccountToken(issuer, new File(SERVICE_ACCOUNT_TOKEN_PATH));
    }

    static String getServiceAccountToken(String issuer, File file) {
        try {
            String token = getServiceAccountToken(file);
            if (token == null) {
                return null;
            }

            JsonWebToken jwt = new JWSInput(token).readJsonContent(JsonWebToken.class);
            if (issuer.equals(jwt.getIssuer())) {
                logger.trace("Including service account token in request");
                return token;
            }
            logger.debug("Not including service account token due to issuer mismatch");
        } catch (Exception e) {
            logger.warn("Failed to read service account token file", e);
        }
        return null;
    }
}
