package org.keycloak.authentication.authenticators.client;

/**
 * Constants used for Kubernetes JWT authentication.
 *
 * @author <a href="mailto:sebastian.laskawiec@defenseunicorns.com">Sebastian Laskawiec</a>
 */
public class KubernetesJWTConstants {

    /**
     * The Kubernetes JWKS URL. This is the standard URL for in-cluster communication.
     */
    public static final String KUBERNETES_JWKS_URL = "https://kubernetes.default.svc/openid/v1/jwks";

    /**
     * The maximum token expiration time in seconds set to 1 hour (plus allowed clock skew). This aligns with default Kubernetes settings.
     */
    public static final int KUBERNETES_MAX_EXPIRATION_TIME_SECONDS = (60 * 60) + JWTClientValidator.ALLOWED_CLOCK_SKEW;

    /**
     * The path to the ServiceAccount token file. This is the standard path for in-cluster communication.
     */
    public static final String KUBERNETES_API_SERVER_ACCESS_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";

}
