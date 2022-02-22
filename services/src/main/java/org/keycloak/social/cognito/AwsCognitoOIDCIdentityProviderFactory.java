package org.keycloak.social.cognito;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class AwsCognitoOIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<AwsCognitoOIDCIdentityProvider> implements SocialIdentityProviderFactory<AwsCognitoOIDCIdentityProvider> {

    public static final String PROVIDER_ID = "awsoidc";

    @Override
    public String getName() {
        return "AWS Cognito";
    }

    @Override
    public AwsCognitoOIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new AwsCognitoOIDCIdentityProvider (session, new OIDCIdentityProviderConfig(model));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, InputStream inputStream) {
        return parseOIDCConfig(session, inputStream);
    }

    @Override
    public OIDCIdentityProviderConfig createConfig() {
        return new OIDCIdentityProviderConfig();
    }

    protected static Map<String, String> parseOIDCConfig(KeycloakSession session, InputStream inputStream) {
        OIDCConfigurationRepresentation rep;
        try {
            rep = JsonSerialization.readValue(inputStream, OIDCConfigurationRepresentation.class);
        } catch (IOException e) {
            throw new RuntimeException("failed to load openid connect metadata", e);
        }
        OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig();
        config.setIssuer(rep.getIssuer());
        config.setLogoutUrl(rep.getLogoutEndpoint());
        config.setAuthorizationUrl(rep.getAuthorizationEndpoint());
        config.setTokenUrl(rep.getTokenEndpoint());
        config.setUserInfoUrl(rep.getUserinfoEndpoint());
        if (rep.getJwksUri() != null) {
            config.setValidateSignature(true);
            config.setUseJwksUrl(true);
            config.setJwksUrl(rep.getJwksUri());
        }
        return config.getConfig();
    }
}