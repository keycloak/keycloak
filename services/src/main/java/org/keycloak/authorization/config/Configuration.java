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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.keycloak.protocol.oidc.OIDCWellKnownProvider.DEFAULT_GRANT_TYPES_SUPPORTED;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Configuration {

    private static final String UMA_VERSION = "1.0";
    private static final List<String> DEFAULT_TOKEN_PROFILES = Arrays.asList("bearer");

    public static final Configuration fromDefault(String authzServerUri,
                                                  String realm,
                                                  URI authorizationEndpoint,
                                                  URI tokenEndpoint, String publicKeyPem) {
        Configuration configuration = new Configuration();

        if (authzServerUri.endsWith("/")) {
            authzServerUri = authzServerUri.substring(0, authzServerUri.lastIndexOf("/"));
        }

        configuration.setVersion(UMA_VERSION);
        configuration.setIssuer(URI.create(authzServerUri));
        configuration.setPatProfiles(DEFAULT_TOKEN_PROFILES);
        configuration.setAatProfiles(DEFAULT_TOKEN_PROFILES);
        configuration.setRptProfiles(DEFAULT_TOKEN_PROFILES);
        configuration.setPatGrantTypes(DEFAULT_GRANT_TYPES_SUPPORTED);
        configuration.setAatGrantTypes(DEFAULT_GRANT_TYPES_SUPPORTED);
        configuration.setTokenEndpoint(tokenEndpoint);
        configuration.setAuthorizationEndpoint(authorizationEndpoint);
        configuration.setResourceSetRegistrationEndpoint(URI.create(authzServerUri + "/authz/protection/resource_set"));
        configuration.setPermissionRegistrationEndpoint(URI.create(authzServerUri + "/authz/protection/permission"));
        configuration.setRptEndpoint(URI.create(authzServerUri + "/authz/authorize"));
        configuration.setRealmPublicKey(publicKeyPem);
        configuration.setServerUrl(URI.create(authzServerUri));
        configuration.setRealm(realm);

        return configuration;
    }

    private String realmPublicKey;
    private String version;
    private URI issuer;

    @JsonProperty("pat_profiles_supported")
    private List<String> patProfiles;

    @JsonProperty("pat_grant_types_supported")
    private List<String> patGrantTypes;

    @JsonProperty("aat_profiles_supported")
    private List<String> aatProfiles;

    @JsonProperty("aat_grant_types_supported")
    private List<String> aatGrantTypes;

    @JsonProperty("rpt_profiles_supported")
    private List<String> rptProfiles;

    @JsonProperty("claim_token_profiles_supported")
    private List<String> claimTokenProfiles;

    @JsonProperty("dynamic_client_endpoint")
    private URI dynamicClientEndpoint;

    @JsonProperty("token_endpoint")
    private URI tokenEndpoint;

    @JsonProperty("authorization_endpoint")
    private URI authorizationEndpoint;

    @JsonProperty("requesting_party_claims_endpoint")
    private URI requestingPartyClaimsEndpoint;

    @JsonProperty("resource_set_registration_endpoint")
    private URI resourceSetRegistrationEndpoint;

    @JsonProperty("introspection_endpoint")
    private URI introspectionEndpoint;

    @JsonProperty("permission_registration_endpoint")
    private URI permissionRegistrationEndpoint;

    @JsonProperty("rpt_endpoint")
    private URI rptEndpoint;

    /**
     * Non-standard, Keycloak specific configuration options
     */
    private String realm;

    private URI serverUrl;

    public String getVersion() {
        return this.version;
    }

    void setVersion(final String version) {
        this.version = version;
    }

    public URI getIssuer() {
        return this.issuer;
    }

    void setIssuer(final URI issuer) {
        this.issuer = issuer;
    }

    public List<String> getPatProfiles() {
        return this.patProfiles;
    }

    void setPatProfiles(final List<String> patProfiles) {
        this.patProfiles = patProfiles;
    }

    public List<String> getPatGrantTypes() {
        return this.patGrantTypes;
    }

    void setPatGrantTypes(final List<String> patGrantTypes) {
        this.patGrantTypes = patGrantTypes;
    }

    public List<String> getAatProfiles() {
        return this.aatProfiles;
    }

    void setAatProfiles(final List<String> aatProfiles) {
        this.aatProfiles = aatProfiles;
    }

    public List<String> getAatGrantTypes() {
        return this.aatGrantTypes;
    }

    void setAatGrantTypes(final List<String> aatGrantTypes) {
        this.aatGrantTypes = aatGrantTypes;
    }

    public List<String> getRptProfiles() {
        return this.rptProfiles;
    }

    void setRptProfiles(final List<String> rptProfiles) {
        this.rptProfiles = rptProfiles;
    }

    public List<String> getClaimTokenProfiles() {
        return this.claimTokenProfiles;
    }

    void setClaimTokenProfiles(final List<String> claimTokenProfiles) {
        this.claimTokenProfiles = claimTokenProfiles;
    }

    public URI getDynamicClientEndpoint() {
        return this.dynamicClientEndpoint;
    }

    void setDynamicClientEndpoint(final URI dynamicClientEndpoint) {
        this.dynamicClientEndpoint = dynamicClientEndpoint;
    }

    public URI getTokenEndpoint() {
        return this.tokenEndpoint;
    }

    void setTokenEndpoint(final URI tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public URI getAuthorizationEndpoint() {
        return this.authorizationEndpoint;
    }

    void setAuthorizationEndpoint(final URI authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public URI getRequestingPartyClaimsEndpoint() {
        return this.requestingPartyClaimsEndpoint;
    }

    void setRequestingPartyClaimsEndpoint(final URI requestingPartyClaimsEndpoint) {
        this.requestingPartyClaimsEndpoint = requestingPartyClaimsEndpoint;
    }

    public URI getResourceSetRegistrationEndpoint() {
        return this.resourceSetRegistrationEndpoint;
    }

    void setResourceSetRegistrationEndpoint(final URI resourceSetRegistrationEndpoint) {
        this.resourceSetRegistrationEndpoint = resourceSetRegistrationEndpoint;
    }

    public URI getIntrospectionEndpoint() {
        return this.introspectionEndpoint;
    }

    void setIntrospectionEndpoint(final URI introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public URI getPermissionRegistrationEndpoint() {
        return this.permissionRegistrationEndpoint;
    }

    void setPermissionRegistrationEndpoint(final URI permissionRegistrationEndpoint) {
        this.permissionRegistrationEndpoint = permissionRegistrationEndpoint;
    }

    public URI getRptEndpoint() {
        return this.rptEndpoint;
    }

    void setRptEndpoint(final URI rptEndpoint) {
        this.rptEndpoint = rptEndpoint;
    }

    public String getRealm() {
        return this.realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public URI getServerUrl() {
        return this.serverUrl;
    }

    public void setServerUrl(URI serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setRealmPublicKey(String realmPublicKey) {
        this.realmPublicKey = realmPublicKey;
    }

    public String getRealmPublicKey() {
        return realmPublicKey;
    }
}
