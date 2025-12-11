/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.config;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.authorization.AuthorizationService;
import org.keycloak.authorization.protection.ProtectionService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UmaConfiguration extends OIDCConfigurationRepresentation {

    public static final UmaConfiguration create(KeycloakSession session) {
        WellKnownProvider oidcProvider = session.getProvider(WellKnownProvider.class, OIDCWellKnownProviderFactory.PROVIDER_ID);
        OIDCConfigurationRepresentation oidcConfig = OIDCConfigurationRepresentation.class.cast(oidcProvider.getConfig());
        UmaConfiguration configuration = new UmaConfiguration();

        configuration.setIssuer(oidcConfig.getIssuer());
        configuration.setAuthorizationEndpoint(oidcConfig.getAuthorizationEndpoint());
        configuration.setTokenEndpoint(oidcConfig.getTokenEndpoint());
        configuration.setJwksUri(oidcConfig.getJwksUri());
        configuration.setRegistrationEndpoint(oidcConfig.getRegistrationEndpoint());
        configuration.setScopesSupported(oidcConfig.getScopesSupported());
        configuration.setResponseTypesSupported(oidcConfig.getResponseTypesSupported());
        configuration.setResponseModesSupported(oidcConfig.getResponseModesSupported());
        configuration.setGrantTypesSupported(oidcConfig.getGrantTypesSupported());
        configuration.setTokenEndpointAuthMethodsSupported(oidcConfig.getTokenEndpointAuthMethodsSupported());
        configuration.setTokenEndpointAuthSigningAlgValuesSupported(oidcConfig.getTokenEndpointAuthSigningAlgValuesSupported());
        configuration.setIntrospectionEndpoint(oidcConfig.getIntrospectionEndpoint());
        configuration.setLogoutEndpoint(oidcConfig.getLogoutEndpoint());

        UriBuilder backendUriBuilder = session.getContext().getUri(UrlType.BACKEND).getBaseUriBuilder();
        RealmModel realm = session.getContext().getRealm();

        configuration.setPermissionEndpoint(backendUriBuilder.clone().path(RealmsResource.class).path(RealmsResource.class, "getAuthorizationService").path(AuthorizationService.class, "getProtectionService").path(ProtectionService.class, "permission").build(realm.getName()).toString());
        configuration.setResourceRegistrationEndpoint(backendUriBuilder.clone().path(RealmsResource.class).path(RealmsResource.class, "getAuthorizationService").path(AuthorizationService.class, "getProtectionService").path(ProtectionService.class, "resource").build(realm.getName()).toString());
        configuration.setPolicyEndpoint(backendUriBuilder.clone().path(RealmsResource.class).path(RealmsResource.class, "getAuthorizationService").path(AuthorizationService.class, "getProtectionService").path(ProtectionService.class, "policy").build(realm.getName()).toString());

        return configuration;
    }

    @JsonProperty("resource_registration_endpoint")
    private String resourceRegistrationEndpoint;

    @JsonProperty("permission_endpoint")
    private String permissionEndpoint;
    
    @JsonProperty("policy_endpoint")
    private String policyEndpoint;

    public String getResourceRegistrationEndpoint() {
        return this.resourceRegistrationEndpoint;
    }

    void setResourceRegistrationEndpoint(String resourceRegistrationEndpoint) {
        this.resourceRegistrationEndpoint = resourceRegistrationEndpoint;
    }

    public String getPermissionEndpoint() {
        return this.permissionEndpoint;
    }

    void setPermissionEndpoint(String permissionEndpoint) {
        this.permissionEndpoint = permissionEndpoint;
    }
    
    public String getPolicyEndpoint() {
        return this.policyEndpoint;
    }

    void setPolicyEndpoint(String policyEndpoint) {
        this.policyEndpoint = policyEndpoint;
    }
}
