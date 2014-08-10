package org.keycloak.adapters;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.keycloak.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.PemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakDeploymentBuilder {
    protected KeycloakDeployment deployment = new KeycloakDeployment();

    protected KeycloakDeploymentBuilder() {
    }


    protected KeycloakDeployment internalBuild(AdapterConfig adapterConfig) {
        if (adapterConfig.getRealm() == null) throw new RuntimeException("Must set 'realm' in config");
        deployment.setRealm(adapterConfig.getRealm());
        String resource = adapterConfig.getResource();
        if (resource == null) throw new RuntimeException("Must set 'resource' in config");
        deployment.setResourceName(resource);

        String realmKeyPem = adapterConfig.getRealmKey();
        if (realmKeyPem != null) {
            PublicKey realmKey = null;
            try {
                realmKey = PemUtils.decodePublicKey(realmKeyPem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            deployment.setRealmKey(realmKey);
        }
        if (adapterConfig.getSslRequired() != null) {
            deployment.setSslRequired(SslRequired.valueOf(adapterConfig.getSslRequired().toUpperCase()));
        } else {
            deployment.setSslRequired(SslRequired.EXTERNAL);
        }
        deployment.setResourceCredentials(adapterConfig.getCredentials());
        deployment.setPublicClient(adapterConfig.isPublicClient());
        deployment.setUseResourceRoleMappings(adapterConfig.isUseResourceRoleMappings());

        if (adapterConfig.isCors()) {
            deployment.setCors(true);
            deployment.setCorsMaxAge(adapterConfig.getCorsMaxAge());
            deployment.setCorsAllowedHeaders(adapterConfig.getCorsAllowedHeaders());
            deployment.setCorsAllowedMethods(adapterConfig.getCorsAllowedMethods());
        }

        deployment.setBearerOnly(adapterConfig.isBearerOnly());

        if (adapterConfig.isBearerOnly()) {
        }

        if (realmKeyPem == null && adapterConfig.isBearerOnly() && adapterConfig.getAuthServerUrl() == null) {
            throw new IllegalArgumentException("For bearer auth, you must set the realm-public-key or auth-server-url");
        }
        if (realmKeyPem == null || !deployment.isBearerOnly()) {
            deployment.setClient(new HttpClientBuilder().build(adapterConfig));
        }
        if (adapterConfig.getAuthServerUrl() == null && (!deployment.isBearerOnly() || realmKeyPem == null)) {
            throw new RuntimeException("You must specify auth-url");
        }
        deployment.setAuthServerBaseUrl(adapterConfig.getAuthServerUrl());
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
