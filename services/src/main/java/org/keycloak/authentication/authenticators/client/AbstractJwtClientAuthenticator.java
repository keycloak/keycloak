/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.authenticators.client;

import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractJwtClientAuthenticator extends AbstractClientAuthenticator{

    public static final String ISSUER_ONLY_AUDIENCE_PROPERTY = "issuer-only-audience";

    public static final String ISSUER_ONLY_AUDIENCE_PROPERTY_DEFAULT = "false";

    private static final List<ProviderConfigProperty> configProperties;

    static {
        ProviderConfigProperty allowAudienceIssuerOnly = new ProviderConfigProperty();
        allowAudienceIssuerOnly.setName(ISSUER_ONLY_AUDIENCE_PROPERTY);
        allowAudienceIssuerOnly.setLabel("Require Issuer-Only Audience");
        allowAudienceIssuerOnly.setHelpText("Requires the audience (aud) claim in private_key_jwt client assertions to be only the OIDC Issuer URL. Enable this for stricter validation compliant with the OIDC core specification. If this is off (default) the Issuer, Token, Introspection, and PAR endpoint URLs are allowed as audience for backwards compatibility.");
        allowAudienceIssuerOnly.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        allowAudienceIssuerOnly.setDefaultValue(ISSUER_ONLY_AUDIENCE_PROPERTY_DEFAULT);
        configProperties = List.of(allowAudienceIssuerOnly);
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.copyOf(configProperties);
    }

    protected boolean isIssuerOnlyAudienceRequired(ClientAuthenticationFlowContext context) {

        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null) {
            return "true".equals(ISSUER_ONLY_AUDIENCE_PROPERTY_DEFAULT);
        }

        Map<String, String> config = authenticatorConfig.getConfig();
        if (config == null) {
            return "true".equals(ISSUER_ONLY_AUDIENCE_PROPERTY_DEFAULT);
        }

        return "true".equals(config.getOrDefault(ISSUER_ONLY_AUDIENCE_PROPERTY, ISSUER_ONLY_AUDIENCE_PROPERTY_DEFAULT));
    }

    protected void validateTokenAudience(ClientAuthenticationFlowContext context, RealmModel realm, JsonWebToken token) {
        List<String> expectedAudiences = getExpectedAudiences(context, realm);
        if (!token.hasAnyAudience(expectedAudiences)) {
            throw new RuntimeException("Token audience doesn't match domain. Expected audiences are any of " + expectedAudiences
                                       + " but audience from token is '" + Arrays.asList(token.getAudience()) + "'");
        }
    }

    protected List<String> getExpectedAudiences(ClientAuthenticationFlowContext context, RealmModel realm) {

        String issuerUrl = Urls.realmIssuer(context.getUriInfo().getBaseUri(), realm.getName());
        if (isIssuerOnlyAudienceRequired(context)) {
            return List.of(issuerUrl);
        }

        String tokenUrl = OIDCLoginProtocolService.tokenUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
        String tokenIntrospectUrl = OIDCLoginProtocolService.tokenIntrospectionUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
        String parEndpointUrl = ParEndpoint.parUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
        List<String> expectedAudiences = new ArrayList<>(Arrays.asList(issuerUrl, tokenUrl, tokenIntrospectUrl, parEndpointUrl));
        String backchannelAuthenticationUrl = CibaGrantType.authorizationUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
        expectedAudiences.add(backchannelAuthenticationUrl);

        return expectedAudiences;
    }
}
