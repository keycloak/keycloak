package org.keycloak.adapters;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.logging.Logger;
import org.keycloak.enums.SslRequired;
import org.keycloak.enums.TokenStore;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.PemUtils;
import org.keycloak.util.SystemPropertiesJsonParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakDeploymentBuilder {

    private static final Logger log = Logger.getLogger(KeycloakDeploymentBuilder.class);

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
            PublicKey realmKey;
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
        if (adapterConfig.getTokenStore() != null) {
            deployment.setTokenStore(TokenStore.valueOf(adapterConfig.getTokenStore().toUpperCase()));
        } else {
            deployment.setTokenStore(TokenStore.SESSION);
        }
        if (adapterConfig.getPrincipalAttribute() != null) deployment.setPrincipalAttribute(adapterConfig.getPrincipalAttribute());
        deployment.setResourceCredentials(adapterConfig.getCredentials());
        deployment.setPublicClient(adapterConfig.isPublicClient());
        deployment.setUseResourceRoleMappings(adapterConfig.isUseResourceRoleMappings());

        deployment.setExposeToken(adapterConfig.isExposeToken());

        if (adapterConfig.isCors()) {
            deployment.setCors(true);
            deployment.setCorsMaxAge(adapterConfig.getCorsMaxAge());
            deployment.setCorsAllowedHeaders(adapterConfig.getCorsAllowedHeaders());
            deployment.setCorsAllowedMethods(adapterConfig.getCorsAllowedMethods());
        }

        deployment.setBearerOnly(adapterConfig.isBearerOnly());
        deployment.setEnableBasicAuth(adapterConfig.isEnableBasicAuth());
        deployment.setAlwaysRefreshToken(adapterConfig.isAlwaysRefreshToken());
        deployment.setRegisterNodeAtStartup(adapterConfig.isRegisterNodeAtStartup());
        deployment.setRegisterNodePeriod(adapterConfig.getRegisterNodePeriod());

        if (realmKeyPem == null && adapterConfig.isBearerOnly() && adapterConfig.getAuthServerUrl() == null) {
            throw new IllegalArgumentException("For bearer auth, you must set the realm-public-key or auth-server-url");
        }
        if (realmKeyPem == null || !deployment.isBearerOnly() || deployment.isRegisterNodeAtStartup() || deployment.getRegisterNodePeriod() != -1) {
            deployment.setClient(new HttpClientBuilder().build(adapterConfig));
        }
        if (adapterConfig.getAuthServerUrl() == null && (!deployment.isBearerOnly() || realmKeyPem == null)) {
            throw new RuntimeException("You must specify auth-url");
        }
        deployment.setAuthServerBaseUrl(adapterConfig);

        log.debug("Use authServerUrl: " + deployment.getAuthServerBaseUrl() + ", tokenUrl: " + deployment.getTokenUrl() + ", relativeUrls: " + deployment.getRelativeUrls());
        return deployment;
    }

    public static KeycloakDeployment build(InputStream is) {
        AdapterConfig adapterConfig = loadAdapterConfig(is);
        return new KeycloakDeploymentBuilder().internalBuild(adapterConfig);
    }

    public static AdapterConfig loadAdapterConfig(InputStream is) {
        ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        AdapterConfig adapterConfig;
        try {
            adapterConfig = mapper.readValue(is, AdapterConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return adapterConfig;
    }


    public static KeycloakDeployment build(AdapterConfig adapterConfig) {
        return new KeycloakDeploymentBuilder().internalBuild(adapterConfig);
    }


}
