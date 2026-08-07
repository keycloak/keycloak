package org.keycloak.broker.kubernetes;

import org.keycloak.broker.oidc.IssuerValidation;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.util.Strings;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

import static org.keycloak.broker.kubernetes.KubernetesConstants.DEFAULT_KUBERNETES_API_SERVER_URL;
import static org.keycloak.broker.kubernetes.KubernetesConstants.DEFAULT_KUBERNETES_ISSUER_URL;
import static org.keycloak.common.util.UriUtils.checkUrl;


public class KubernetesIdentityProviderConfig extends IdentityProviderModel implements IssuerValidation {

    private static final Logger logger = Logger.getLogger(KubernetesIdentityProviderConfig.class);

    public static final String AUTOMATIC_ISSUER_DISCOVERY = "automaticIssuerDiscovery";
    public static final String ISSUER_DISCOVERY_URL = "issuerDiscoveryUrl";

    public KubernetesIdentityProviderConfig() {
    }

    public KubernetesIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getIssuer() {
        String issuer = getConfig().get(ISSUER);
        if (Strings.isEmpty(issuer)) {
            return DEFAULT_KUBERNETES_ISSUER_URL;
        }

        return issuer;
    }

    public boolean isAutomaticIssuerDiscovery() {
        return !Boolean.FALSE.toString().equals(getConfig().get(AUTOMATIC_ISSUER_DISCOVERY));
    }

    public String getIssuerDiscoveryUrl() {
        String issuerDiscoveryUrl = getConfig().get(ISSUER_DISCOVERY_URL);
        if (Strings.isEmpty(issuerDiscoveryUrl)) {
            return DEFAULT_KUBERNETES_API_SERVER_URL;
        }

        return issuerDiscoveryUrl;
    }

    public int getAllowedClockSkew() {
        String allowedClockSkew = getConfig().get(ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew == null || allowedClockSkew.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(getConfig().get(ALLOWED_CLOCK_SKEW));
        } catch (NumberFormatException e) {
            // ignore it and use default
            return 0;
        }
    }

    @Override
    public Boolean isHideOnLogin() {
        return true;
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);

        String issuer = Strings.isEmpty(getConfig().get(ISSUER)) && isAutomaticIssuerDiscovery()
                ? resolveIssuer(realm)
                : getIssuer();

        getConfig().put(ISSUER, issuer);
        validateIssuer(realm, IdentityProviderType.CLIENT_ASSERTION);
    }

    private String resolveIssuer(RealmModel realm) {
        String issuerDiscoveryUrl = getIssuerDiscoveryUrl();
        checkUrl(realm.getSslRequired(), issuerDiscoveryUrl, ISSUER_DISCOVERY_URL);

        boolean trustedKubernetesApiUrl = KubernetesUtils.isTrustedKubernetesApiUrl(issuerDiscoveryUrl);

        try {
            KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
            SimpleHttpRequest request = SimpleHttp.create(session)
                    .doGet(KubernetesUtils.discoveryUrl(issuerDiscoveryUrl))
                    .acceptJson();

            String token = getServiceAccountToken();
            if (trustedKubernetesApiUrl && !Strings.isEmpty(token)) {
                // Only send the pod token to the in-cluster Kubernetes API, never to external OIDC endpoints.
                request.auth(token);
            }

            OIDCConfigurationRepresentation oidcConfig;
            try (SimpleHttpResponse response = request.asResponse()) {
                int status = response.getStatus();
                if (status != 200) {
                    throw new IllegalArgumentException(String.format("Failed to resolve Kubernetes issuer from '%s': HTTP status %d", issuerDiscoveryUrl, status));
                }
                oidcConfig = response.asJson(OIDCConfigurationRepresentation.class);
            }
            if (Strings.isEmpty(oidcConfig.getIssuer())) {
                throw new IllegalArgumentException(String.format("Could not resolve issuer from '%s'", issuerDiscoveryUrl));
            }

            return oidcConfig.getIssuer();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Failed to resolve Kubernetes issuer from '%s'", issuerDiscoveryUrl), e);
        }
    }

    private String getServiceAccountToken() {
        try {
            return KubernetesUtils.getServiceAccountToken();
        } catch (Exception e) {
            logger.warn("Failed to read service account token file", e);
            return null;
        }
    }
}
