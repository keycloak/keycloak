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

import org.jboss.logging.Logger;
import org.keycloak.Config;
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

public abstract class AbstractJwtClientAuthenticator extends AbstractClientAuthenticator {

    private static final Logger LOG = Logger.getLogger(AbstractJwtClientAuthenticator.class);

    public static final String LENIENT_AUDIENCE_VALIDATION_PROPERTY = "lenient-audience-validation";

    public static final boolean LENIENT_AUDIENCE_VALIDATION_DEFAULT_VALUE = false;

    protected boolean useLenientAudienceValidation;

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty allowAudienceIssuerOnly = new ProviderConfigProperty();
        allowAudienceIssuerOnly.setName(LENIENT_AUDIENCE_VALIDATION_PROPERTY);
        allowAudienceIssuerOnly.setLabel("Lenient Audience Validation");
        allowAudienceIssuerOnly.setHelpText("If 'off' requires the audience (aud) claim in private_key_jwt client assertions to be only the OIDC Issuer URL. If this is 'on' the Issuer, Token, Introspection, and PAR endpoint URLs are allowed as audience for backwards compatibility.");
        allowAudienceIssuerOnly.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        allowAudienceIssuerOnly.setDefaultValue(useLenientAudienceValidation);
        return List.of(allowAudienceIssuerOnly);
    }

    protected boolean isIssuerOnlyAudienceRequired(ClientAuthenticationFlowContext context) {

        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null) {
            return !isUseLenientAudienceValidation();
        }

        Map<String, String> config = authenticatorConfig.getConfig();
        if (config == null) {
            return !isUseLenientAudienceValidation();
        }

        return "false".equals(config.getOrDefault(LENIENT_AUDIENCE_VALIDATION_PROPERTY, Boolean.toString(isUseLenientAudienceValidation())));
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

    @Override
    public void init(Config.Scope config) {
        super.init(config);

        // This allows to override the server-wide default for JWT client assertion based audience validation in client authentication, via command line options.
        // to explicitly use the lenient audience validation, one can use:
        // --spi-client-authenticator-client-jwt-lenient-audience-validation=true
        // --spi-client-authenticator-client-secret-jwt-lenient-audience-validation=true
        // if nothing is configured, we use the secure default which restricts the audience to be the issuer only audience.
        this.useLenientAudienceValidation = config.getBoolean("lenient-audience-validation", LENIENT_AUDIENCE_VALIDATION_DEFAULT_VALUE);

        if (useLenientAudienceValidation) {
            LOG.warnf("The client authentication '%s' with JWT based client assertions is configured as 'lenient' to allow a broader audience for backward compatibility (issuerUrl, tokenUrl, tokenIntrospectUrl, parEndpointUrl, backchannelAuthenticationUrl). It is recommended that the JWT client authentication configurations be adjusted to allow only the realm issuer in the audience.", getId());
        }
    }

    public boolean isUseLenientAudienceValidation() {
        return useLenientAudienceValidation;
    }
}
