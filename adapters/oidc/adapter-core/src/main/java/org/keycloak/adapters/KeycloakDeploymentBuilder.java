/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.rotation.HardcodedPublicKeyLocator;
import org.keycloak.adapters.rotation.JWKPublicKeyLocator;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.PemUtils;
import org.keycloak.enums.TokenStore;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.util.SystemPropertiesJsonParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:brad.culley@spartasystems.com">Brad Culley</a>
 * @author <a href="mailto:john.ament@spartasystems.com">John D. Ament</a>
 * @version $Revision: 1 $
 */
public class KeycloakDeploymentBuilder {

    private static final Logger log = Logger.getLogger(KeycloakDeploymentBuilder.class);

    protected KeycloakDeployment deployment = new KeycloakDeployment();

    protected KeycloakDeploymentBuilder() {
    }


    protected KeycloakDeployment internalBuild(final AdapterConfig adapterConfig) {
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
                HardcodedPublicKeyLocator pkLocator = new HardcodedPublicKeyLocator(realmKey);
                deployment.setPublicKeyLocator(pkLocator);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            JWKPublicKeyLocator pkLocator = new JWKPublicKeyLocator();
            deployment.setPublicKeyLocator(pkLocator);
        }

        if (adapterConfig.getSslRequired() != null) {
            deployment.setSslRequired(SslRequired.valueOf(adapterConfig.getSslRequired().toUpperCase()));
        } else {
            deployment.setSslRequired(SslRequired.EXTERNAL);
        }

        if (adapterConfig.getConfidentialPort() != -1) {
            deployment.setConfidentialPort(adapterConfig.getConfidentialPort());
        }

        if (adapterConfig.getTokenStore() != null) {
            deployment.setTokenStore(TokenStore.valueOf(adapterConfig.getTokenStore().toUpperCase()));
        } else {
            deployment.setTokenStore(TokenStore.SESSION);
        }
        if (adapterConfig.getTokenCookiePath() != null) {
            deployment.setAdapterStateCookiePath(adapterConfig.getTokenCookiePath());
        }
        if (adapterConfig.getPrincipalAttribute() != null) deployment.setPrincipalAttribute(adapterConfig.getPrincipalAttribute());

        deployment.setResourceCredentials(adapterConfig.getCredentials());
        deployment.setClientAuthenticator(ClientCredentialsProviderUtils.bootstrapClientAuthenticator(deployment));

        deployment.setPublicClient(adapterConfig.isPublicClient());
        deployment.setUseResourceRoleMappings(adapterConfig.isUseResourceRoleMappings());

        deployment.setExposeToken(adapterConfig.isExposeToken());

        if (adapterConfig.isCors()) {
            deployment.setCors(true);
            deployment.setCorsMaxAge(adapterConfig.getCorsMaxAge());
            deployment.setCorsAllowedHeaders(adapterConfig.getCorsAllowedHeaders());
            deployment.setCorsAllowedMethods(adapterConfig.getCorsAllowedMethods());
            deployment.setCorsExposedHeaders(adapterConfig.getCorsExposedHeaders());
        }

        // https://tools.ietf.org/html/rfc7636
        if (adapterConfig.isPkce()) {
            deployment.setPkce(true);
        }

        deployment.setBearerOnly(adapterConfig.isBearerOnly());
        deployment.setAutodetectBearerOnly(adapterConfig.isAutodetectBearerOnly());
        deployment.setEnableBasicAuth(adapterConfig.isEnableBasicAuth());
        deployment.setAlwaysRefreshToken(adapterConfig.isAlwaysRefreshToken());
        deployment.setRegisterNodeAtStartup(adapterConfig.isRegisterNodeAtStartup());
        deployment.setRegisterNodePeriod(adapterConfig.getRegisterNodePeriod());
        deployment.setTokenMinimumTimeToLive(adapterConfig.getTokenMinimumTimeToLive());
        deployment.setMinTimeBetweenJwksRequests(adapterConfig.getMinTimeBetweenJwksRequests());
        deployment.setPublicKeyCacheTtl(adapterConfig.getPublicKeyCacheTtl());
        deployment.setIgnoreOAuthQueryParameter(adapterConfig.isIgnoreOAuthQueryParameter());
        deployment.setRewriteRedirectRules(adapterConfig.getRedirectRewriteRules());
        deployment.setVerifyTokenAudience(adapterConfig.isVerifyTokenAudience());

        if (realmKeyPem == null && adapterConfig.isBearerOnly() && adapterConfig.getAuthServerUrl() == null) {
            throw new IllegalArgumentException("For bearer auth, you must set the realm-public-key or auth-server-url");
        }
        if (adapterConfig.getAuthServerUrl() == null && (!deployment.isBearerOnly() || realmKeyPem == null)) {
            throw new RuntimeException("You must specify auth-server-url");
        }
        deployment.setClient(createHttpClientProducer(adapterConfig));
        deployment.setAuthServerBaseUrl(adapterConfig);
        if (adapterConfig.getTurnOffChangeSessionIdOnLogin() != null) {
            deployment.setTurnOffChangeSessionIdOnLogin(adapterConfig.getTurnOffChangeSessionIdOnLogin());
        }

        final PolicyEnforcerConfig policyEnforcerConfig = adapterConfig.getPolicyEnforcerConfig();

        if (policyEnforcerConfig != null) {
            deployment.setPolicyEnforcer(new Callable<PolicyEnforcer>() {
                PolicyEnforcer policyEnforcer;
                @Override
                public PolicyEnforcer call() {
                    if (policyEnforcer == null) {
                        synchronized (deployment) {
                            if (policyEnforcer == null) {
                                policyEnforcer = new PolicyEnforcer(deployment, adapterConfig);
                            }
                        }
                    }
                    return policyEnforcer;
                }
            });
        }

        return deployment;
    }

    private Callable<HttpClient> createHttpClientProducer(final AdapterConfig adapterConfig) {
        return new Callable<HttpClient>() {
            private HttpClient client;
            @Override
            public HttpClient call() {
                if (client == null) {
                    synchronized (deployment) {
                        if (client == null) {
                            client = new HttpClientBuilder().build(adapterConfig);
                        }
                    }
                }
                return client;
            }
        };
    }

    public static KeycloakDeployment build(InputStream is) {
        CryptoIntegration.init(KeycloakDeploymentBuilder.class.getClassLoader());
        AdapterConfig adapterConfig = loadAdapterConfig(is);
        return new KeycloakDeploymentBuilder().internalBuild(adapterConfig);
    }

    public static AdapterConfig loadAdapterConfig(InputStream is) {
        ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
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
