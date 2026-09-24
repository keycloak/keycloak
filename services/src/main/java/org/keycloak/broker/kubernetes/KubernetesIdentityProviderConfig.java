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

import static org.keycloak.broker.kubernetes.KubernetesConstants.DEFAULT_KUBERNETES_API_SERVER_URL;
import static org.keycloak.broker.kubernetes.KubernetesConstants.DEFAULT_KUBERNETES_ISSUER_URL;
import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.JWKS_URL;
import static org.keycloak.common.util.UriUtils.checkUrl;


public class KubernetesIdentityProviderConfig extends IdentityProviderModel implements IssuerValidation {

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

    public String getIssuerDiscoveryUrl() {
        return getConfig().get(ISSUER_DISCOVERY_URL);
    }

    public String getJwksUrl() {
        return getConfig().get(JWKS_URL);
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

        String issuerDiscoveryUrl = getIssuerDiscoveryUrl();
        if (!Strings.isEmpty(issuerDiscoveryUrl)) {
            resolveIssuer(realm, issuerDiscoveryUrl);
        } else if (Strings.isEmpty(getConfig().get(ISSUER))) {
            getConfig().put(ISSUER, getIssuer());
        }

        if (Strings.isEmpty(issuerDiscoveryUrl)) {
            getConfig().remove(JWKS_URL);
        }

        validateIssuer(realm, IdentityProviderType.CLIENT_ASSERTION);
    }

    private void resolveIssuer(RealmModel realm, String issuerDiscoveryUrl) {
        checkUrl(realm.getSslRequired(), issuerDiscoveryUrl, ISSUER_DISCOVERY_URL);

        try {
            KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
            SimpleHttpRequest request = SimpleHttp.create(session)
                    .doGet(KubernetesUtils.discoveryUrl(issuerDiscoveryUrl))
                    .acceptJson();

            if (KubernetesUtils.discoveryUrl(DEFAULT_KUBERNETES_API_SERVER_URL)
                    .equals(KubernetesUtils.discoveryUrl(issuerDiscoveryUrl))) {
                String token = KubernetesUtils.getServiceAccountToken();
                if (!Strings.isEmpty(token)) {
                    request.auth(token);
                }
            }

            OIDCConfigurationRepresentation oidcConfig;
            try (SimpleHttpResponse response = request.asResponse()) {
                int status = response.getStatus();
                if (status != 200) {
                    throw new IllegalArgumentException(String.format("Failed to resolve Kubernetes issuer from '%s': HTTP status %d", issuerDiscoveryUrl, status));
                }
                oidcConfig = response.asJson(OIDCConfigurationRepresentation.class);
            }
            if (Strings.isEmpty(oidcConfig.getIssuer()) || Strings.isEmpty(oidcConfig.getJwksUri())) {
                throw new IllegalArgumentException(String.format("Could not resolve issuer and JWKS URL from '%s'", issuerDiscoveryUrl));
            }

            checkUrl(realm.getSslRequired(), oidcConfig.getIssuer(), ISSUER);
            checkUrl(realm.getSslRequired(), oidcConfig.getJwksUri(), JWKS_URL);
            getConfig().put(ISSUER, oidcConfig.getIssuer());
            getConfig().put(JWKS_URL, oidcConfig.getJwksUri());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Failed to resolve Kubernetes issuer from '%s'", issuerDiscoveryUrl), e);
        }
    }
}
