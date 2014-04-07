package org.keycloak.adapters;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.keycloak.OAuth2Constants;
import org.keycloak.ServiceUrlConstants;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.EnvUtil;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.KeystoreUtil;
import org.keycloak.util.PemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakDeploymentBuilder {
    protected KeycloakDeployment deployment = new KeycloakDeployment();

    protected KeycloakDeploymentBuilder() {}



    protected KeycloakDeployment internalBuild(AdapterConfig adapterConfig) {

        if (adapterConfig.getRealm() == null) throw new RuntimeException("Must set 'realm' in config");
        deployment.setRealm(adapterConfig.getRealm());
        String resource = adapterConfig.getResource();
        if (resource == null) throw new RuntimeException("Must set 'resource' in config");
        deployment.setResourceName(resource);

        String realmKeyPem = adapterConfig.getRealmKey();
        if (realmKeyPem == null) {
            throw new IllegalArgumentException("You must set the realm-public-key");
        }

        PublicKey realmKey = null;
        try {
            realmKey = PemUtils.decodePublicKey(realmKeyPem);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        deployment.setRealmKey(realmKey);
        deployment.setSslRequired(!adapterConfig.isSslNotRequired());
        deployment.setResourceCredentials(adapterConfig.getCredentials());
        deployment.setPublicClient(adapterConfig.isPublicClient());
        deployment.setUseResourceRoleMappings(adapterConfig.isUseResourceRoleMappings());

        if (adapterConfig.isBearerOnly()) {
            deployment.setBearerOnly(true);
            return deployment;
        }

        deployment.setClient(new HttpClientBuilder().build(adapterConfig));
        if (adapterConfig.getAuthServerUrl() == null) {
            throw new RuntimeException("You must specify auth-url");
        }
        KeycloakUriBuilder serverBuilder = KeycloakUriBuilder.fromUri(adapterConfig.getAuthServerUrl());
        String authUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGIN_PATH).build(adapterConfig.getRealm()).toString();
        String tokenUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_ACCESS_CODE_PATH).build(adapterConfig.getRealm()).toString();
        String refreshUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_REFRESH_PATH).build(adapterConfig.getRealm()).toString();
        String logoutUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build(adapterConfig.getRealm()).toString();
        String accountUrl = serverBuilder.clone().path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH).build(adapterConfig.getRealm()).toString();

        deployment.setAuthUrl(KeycloakUriBuilder.fromUri(authUrl).queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName()));
        deployment.setCodeUrl(tokenUrl);
        deployment.setRefreshUrl(refreshUrl);
        deployment.setLogoutUrl(KeycloakUriBuilder.fromUri(logoutUrl));
        deployment.setAccountUrl(accountUrl);

        if (adapterConfig.isCors()) {
            deployment.setCors(true);
            deployment.setCorsMaxAge(adapterConfig.getCorsMaxAge());
            deployment.setCorsAllowedHeaders(adapterConfig.getCorsAllowedHeaders());
            deployment.setCorsAllowedMethods(adapterConfig.getCorsAllowedMethods());
        }

        return deployment;
    }

    public static KeycloakDeployment build(InputStream is) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        AdapterConfig adapterConfig = null;
        try {
            adapterConfig = mapper.readValue(is, AdapterConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new KeycloakDeploymentBuilder().internalBuild(adapterConfig);
    }


    public static KeycloakDeployment build(AdapterConfig adapterConfig) {
        return new KeycloakDeploymentBuilder().internalBuild(adapterConfig);
    }


}
