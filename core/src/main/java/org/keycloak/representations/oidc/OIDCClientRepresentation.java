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

package org.keycloak.representations.oidc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class OIDCClientRepresentation {

    private List<String> redirect_uris;

    private String token_endpoint_auth_method;

    private List<String> grant_types;

    private List<String> response_types;

    private String client_id;

    private String client_secret;

    private String client_name;

    private String client_uri;

    private String logo_uri;

    private String scope;

    private String contacts;

    private String tos_uri;

    private String policy_uri;

    private String jwks_uri;

    private String jwks;

    private String software_id;

    private String software_version;

    private Integer client_id_issued_at;

    private Integer client_secret_expires_at;

    private String registration_client_uri;

    private String registration_access_token;

    public List<String> getRedirectUris() {
        return redirect_uris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirect_uris = redirectUris;
    }

    public String getTokenEndpointAuthMethod() {
        return token_endpoint_auth_method;
    }

    public void setTokenEndpointAuthMethod(String token_endpoint_auth_method) {
        this.token_endpoint_auth_method = token_endpoint_auth_method;
    }

    public List<String> getGrantTypes() {
        return grant_types;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grant_types = grantTypes;
    }

    public List<String> getResponseTypes() {
        return response_types;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.response_types = responseTypes;
    }

    public String getClientId() {
        return client_id;
    }

    public void setClientId(String clientId) {
        this.client_id = clientId;
    }

    public String getClientSecret() {
        return client_secret;
    }

    public void setClientSecret(String clientSecret) {
        this.client_secret = clientSecret;
    }

    public String getClientName() {
        return client_name;
    }

    public void setClientName(String client_name) {
        this.client_name = client_name;
    }

    public String getClientUri() {
        return client_uri;
    }

    public void setClientUri(String client_uri) {
        this.client_uri = client_uri;
    }

    public String getLogoUri() {
        return logo_uri;
    }

    public void setLogoUri(String logo_uri) {
        this.logo_uri = logo_uri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getContacts() {
        return contacts;
    }

    public void setContacts(String contacts) {
        this.contacts = contacts;
    }

    public String getTosUri() {
        return tos_uri;
    }

    public void setTosUri(String tos_uri) {
        this.tos_uri = tos_uri;
    }

    public String getPolicyUri() {
        return policy_uri;
    }

    public void setPolicyUri(String policy_uri) {
        this.policy_uri = policy_uri;
    }

    public String getJwksUri() {
        return jwks_uri;
    }

    public void setJwksUri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getSoftwareId() {
        return software_id;
    }

    public void setSoftwareId(String softwareId) {
        this.software_id = softwareId;
    }

    public String getSoftwareVersion() {
        return software_version;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.software_version = softwareVersion;
    }

    public Integer getClientIdIssuedAt() {
        return client_id_issued_at;
    }

    public void setClientIdIssuedAt(Integer clientIdIssuedAt) {
        this.client_id_issued_at = clientIdIssuedAt;
    }

    public Integer getClientSecretExpiresAt() {
        return client_secret_expires_at;
    }

    public void setClientSecretExpiresAt(Integer client_secret_expires_at) {
        this.client_secret_expires_at = client_secret_expires_at;
    }

    public String getRegistrationClientUri() {
        return registration_client_uri;
    }

    public void setRegistrationClientUri(String registrationClientUri) {
        this.registration_client_uri = registrationClientUri;
    }

    public String getRegistrationAccessToken() {
        return registration_access_token;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registration_access_token = registrationAccessToken;
    }

}
