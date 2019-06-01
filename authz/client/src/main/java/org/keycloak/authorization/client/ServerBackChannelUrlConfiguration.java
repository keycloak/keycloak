/*
 *  Copyright 2019 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.client;

import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;

import java.util.List;

class ServerBackChannelUrlConfiguration extends ServerConfiguration {

    private static final String TOKEN_INTROSPECTION_PATH = "/realms/{realm-name}/protocol/openid-connect/token/introspect";
    private static final String USER_INFO_PATH = "/realms/{realm-name}/protocol/openid-connect/userinfo";

    private static final String REGISTRATION_PATH = "/realms/{realm-name}/clients-registrations/openid-connect";
    private static final String RESOURCE_REGISTRATION_PATH = "/realms/{realm-name}/authz/protection/resource_set";
    private static final String PERMISSION_PATH = "/realms/{realm-name}/authz/protection/permission";
    private static final String POLICY_PATH = "/realms/{realm-name}/authz/protection/uma-policy";

    private final ServerConfiguration delegate;


    private String tokenUrl;

    private String tokenIntrospectionUrl;

    private String userInfoUrl;

    private String logoutUrl;

    private String jwksUrl;

    private String registrationUrl;

    private String resourceRegistrationUrl;

    private String permissionUrl;

    private String policyUrl;


    ServerBackChannelUrlConfiguration(ServerConfiguration delegate, String authServerUrl, String realm) {
        this.delegate = delegate;
        resolveBackChannelUrls(authServerUrl, realm);
    }


    private void resolveBackChannelUrls(String backChannelUrl, String realm) {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(backChannelUrl);
        tokenUrl = builder.clone().path(ServiceUrlConstants.TOKEN_PATH).build(realm).toString();
        tokenIntrospectionUrl = builder.clone().path(TOKEN_INTROSPECTION_PATH).build(realm).toString();
        userInfoUrl = builder.clone().path(USER_INFO_PATH).build(realm).toString();
        logoutUrl = builder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build(realm).toString();
        jwksUrl = builder.clone().path(ServiceUrlConstants.JWKS_URL).build(realm).toString();
        registrationUrl = builder.clone().path(REGISTRATION_PATH).build(realm).toString();
        resourceRegistrationUrl = builder.clone().path(RESOURCE_REGISTRATION_PATH).build(realm).toString();
        permissionUrl = builder.clone().path(PERMISSION_PATH).build(realm).toString();
        policyUrl = builder.clone().path(POLICY_PATH).build(realm).toString();
    }

    @Override
    public String getTokenEndpoint() {
        return tokenUrl;
    }

    @Override
    public String getTokenIntrospectionEndpoint() {
        return tokenIntrospectionUrl;
    }

    @Override
    public String getUserinfoEndpoint() {
        return userInfoUrl;
    }

    @Override
    public String getLogoutEndpoint() {
        return logoutUrl;
    }

    @Override
    public String getJwksUri() {
        return jwksUrl;
    }

    @Override
    public String getRegistrationEndpoint() {
        return registrationUrl;
    }

    @Override
    public String getResourceRegistrationEndpoint() {
        return resourceRegistrationUrl;
    }

    @Override
    public String getPermissionEndpoint() {
        return permissionUrl;
    }

    @Override
    public String getPolicyEndpoint() {
        return policyUrl;
    }


    @Override
    public String getIssuer() {
        return delegate.getIssuer();
    }

    @Override
    public String getAuthorizationEndpoint() {
        return delegate.getAuthorizationEndpoint();
    }

    @Override
    public String getCheckSessionIframe() {
        return delegate.getCheckSessionIframe();
    }

    @Override
    public List<String> getGrantTypesSupported() {
        return delegate.getGrantTypesSupported();
    }

    @Override
    public List<String> getResponseTypesSupported() {
        return delegate.getResponseTypesSupported();
    }

    @Override
    public List<String> getSubjectTypesSupported() {
        return delegate.getSubjectTypesSupported();
    }

    @Override
    public List<String> getIdTokenSigningAlgValuesSupported() {
        return delegate.getIdTokenSigningAlgValuesSupported();
    }

    @Override
    public List<String> getUserInfoSigningAlgValuesSupported() {
        return delegate.getUserInfoSigningAlgValuesSupported();
    }

    @Override
    public List<String> getRequestObjectSigningAlgValuesSupported() {
        return delegate.getRequestObjectSigningAlgValuesSupported();
    }

    @Override
    public List<String> getResponseModesSupported() {
        return delegate.getResponseModesSupported();
    }

    @Override
    public List<String> getTokenEndpointAuthMethodsSupported() {
        return delegate.getTokenEndpointAuthMethodsSupported();
    }

    @Override
    public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return delegate.getTokenEndpointAuthSigningAlgValuesSupported();
    }

    @Override
    public List<String> getClaimsSupported() {
        return delegate.getClaimsSupported();
    }

    @Override
    public List<String> getClaimTypesSupported() {
        return delegate.getClaimTypesSupported();
    }

    @Override
    public Boolean getClaimsParameterSupported() {
        return delegate.getClaimsParameterSupported();
    }

    @Override
    public List<String> getScopesSupported() {
        return delegate.getScopesSupported();
    }

    @Override
    public Boolean getRequestParameterSupported() {
        return delegate.getRequestParameterSupported();
    }

    @Override
    public Boolean getRequestUriParameterSupported() {
        return delegate.getRequestUriParameterSupported();
    }
}
