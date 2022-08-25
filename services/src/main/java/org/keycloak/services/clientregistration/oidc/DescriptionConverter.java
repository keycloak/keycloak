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

package org.keycloak.services.clientregistration.oidc;

import com.google.common.collect.Streams;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ParConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationException;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.CibaConfig.OIDC_CIBA_GRANT_ENABLED;
import static org.keycloak.models.OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DescriptionConverter {

    public static ClientRepresentation toInternal(KeycloakSession session, OIDCClientRepresentation clientOIDC) throws ClientRegistrationException {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientOIDC.getClientId());
        client.setName(clientOIDC.getClientName());
        client.setRedirectUris(clientOIDC.getRedirectUris());
        client.setBaseUrl(clientOIDC.getClientUri());
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        String scopeParam = clientOIDC.getScope();
        if (scopeParam != null) client.setOptionalClientScopes(new ArrayList<>(Arrays.asList(scopeParam.split(" "))));

        List<String> oidcResponseTypes = clientOIDC.getResponseTypes();
        if (oidcResponseTypes == null || oidcResponseTypes.isEmpty()) {
            oidcResponseTypes = Collections.singletonList(OIDCResponseType.CODE);
        }
        List<String> oidcGrantTypes = clientOIDC.getGrantTypes();

        try {
            OIDCResponseType responseType = OIDCResponseType.parse(oidcResponseTypes);
            client.setStandardFlowEnabled(responseType.hasResponseType(OIDCResponseType.CODE));
            client.setImplicitFlowEnabled(responseType.isImplicitOrHybridFlow());

            if (oidcGrantTypes != null) {
                client.setDirectAccessGrantsEnabled(oidcGrantTypes.contains(OAuth2Constants.PASSWORD));
                client.setServiceAccountsEnabled(oidcGrantTypes.contains(OAuth2Constants.CLIENT_CREDENTIALS));
                setOidcCibaGrantEnabled(client, oidcGrantTypes.contains(OAuth2Constants.CIBA_GRANT_TYPE));
            }
        } catch (IllegalArgumentException iae) {
            throw new ClientRegistrationException(iae.getMessage(), iae);
        }

        String authMethod = clientOIDC.getTokenEndpointAuthMethod();
        client.setPublicClient(Boolean.FALSE);
        if ("none".equals(authMethod)) {
            client.setClientAuthenticatorType("none");
            client.setPublicClient(Boolean.TRUE);
        } else {
            ClientAuthenticatorFactory clientAuthFactory;
            if (authMethod == null) {
                clientAuthFactory = (ClientAuthenticatorFactory) session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, KeycloakModelUtils.getDefaultClientAuthenticatorType());
            } else {
                clientAuthFactory = AuthorizeClientUtil.findClientAuthenticatorForOIDCAuthMethod(session, authMethod);
            }

            if (clientAuthFactory == null) {
                throw new ClientRegistrationException("Not found clientAuthenticator for requested token_endpoint_auth_method");
            }
            client.setClientAuthenticatorType(clientAuthFactory.getId());
        }

        boolean publicKeySet = setPublicKey(clientOIDC, client);
        if (authMethod != null && authMethod.equals(OIDCLoginProtocol.PRIVATE_KEY_JWT) && !publicKeySet) {
            throw new ClientRegistrationException("Didn't find key of supported keyType for use " + JWK.Use.SIG.asString());
        }

        OIDCAdvancedConfigWrapper configWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        if (clientOIDC.getUserinfoSignedResponseAlg() != null) {
            configWrapper.setUserInfoSignedResponseAlg(clientOIDC.getUserinfoSignedResponseAlg());
        }

        if (clientOIDC.getRequestObjectSigningAlg() != null) {
            configWrapper.setRequestObjectSignatureAlg(clientOIDC.getRequestObjectSigningAlg());
        }

        if (clientOIDC.getUserinfoEncryptedResponseAlg() != null) {
            configWrapper.setUserInfoEncryptedResponseAlg(clientOIDC.getUserinfoEncryptedResponseAlg());
        }

        if (clientOIDC.getUserinfoEncryptedResponseEnc() != null) {
            configWrapper.setUserInfoEncryptedResponseEnc(clientOIDC.getUserinfoEncryptedResponseEnc());
        }

        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.5
        Boolean tlsClientCertificateBoundAccessTokens = clientOIDC.getTlsClientCertificateBoundAccessTokens();
        if (tlsClientCertificateBoundAccessTokens != null) {
            if (tlsClientCertificateBoundAccessTokens.booleanValue()) configWrapper.setUseMtlsHoKToken(true);
            else configWrapper.setUseMtlsHoKToken(false);
        }

        if (clientOIDC.getTlsClientAuthSubjectDn() != null) {
            configWrapper.setTlsClientAuthSubjectDn(clientOIDC.getTlsClientAuthSubjectDn());

            // According to specification, attribute tls_client_auth_subject_dn has subject DN in the exact expected format. There is no reason for support regex comparisons
            configWrapper.setAllowRegexPatternComparison(false);
        }

        if (clientOIDC.getIdTokenSignedResponseAlg() != null) {
            configWrapper.setIdTokenSignedResponseAlg(clientOIDC.getIdTokenSignedResponseAlg());
        }

        if (clientOIDC.getIdTokenEncryptedResponseAlg() != null) {
            configWrapper.setIdTokenEncryptedResponseAlg(clientOIDC.getIdTokenEncryptedResponseAlg());
        }

        if (clientOIDC.getIdTokenEncryptedResponseEnc() != null) {
            configWrapper.setIdTokenEncryptedResponseEnc(clientOIDC.getIdTokenEncryptedResponseEnc());
        }

        configWrapper.setAuthorizationSignedResponseAlg(clientOIDC.getAuthorizationSignedResponseAlg());
        configWrapper.setAuthorizationEncryptedResponseAlg(clientOIDC.getAuthorizationEncryptedResponseAlg());
        configWrapper.setAuthorizationEncryptedResponseEnc(clientOIDC.getAuthorizationEncryptedResponseEnc());

        if (clientOIDC.getRequestUris() != null) {
            configWrapper.setRequestUris(clientOIDC.getRequestUris());
        }

        configWrapper.setTokenEndpointAuthSigningAlg(clientOIDC.getTokenEndpointAuthSigningAlg());

        configWrapper.setBackchannelLogoutUrl(clientOIDC.getBackchannelLogoutUri());

        if (clientOIDC.getBackchannelLogoutSessionRequired() == null) {
            configWrapper.setBackchannelLogoutSessionRequired(true);
        } else {
            configWrapper.setBackchannelLogoutSessionRequired(clientOIDC.getBackchannelLogoutSessionRequired());
        }

        if (clientOIDC.getBackchannelLogoutRevokeOfflineTokens() == null) {
            configWrapper.setBackchannelLogoutRevokeOfflineTokens(false);
        } else {
            configWrapper.setBackchannelLogoutRevokeOfflineTokens(clientOIDC.getBackchannelLogoutRevokeOfflineTokens());
        }

        if (clientOIDC.getLogoUri() != null) {
            configWrapper.setLogoUri(clientOIDC.getLogoUri());
        }

        if (clientOIDC.getPolicyUri() != null) {
            configWrapper.setPolicyUri(clientOIDC.getPolicyUri());
        }

        if (clientOIDC.getTosUri() != null) {
            configWrapper.setTosUri(clientOIDC.getTosUri());
        }

        if (clientOIDC.getPostLogoutRedirectUris() != null) {
            configWrapper.setPostLogoutRedirectUris(clientOIDC.getPostLogoutRedirectUris());
        }

        // CIBA
        String backchannelTokenDeliveryMode = clientOIDC.getBackchannelTokenDeliveryMode();
        if (backchannelTokenDeliveryMode != null) {
            Map<String, String> attr = Optional.ofNullable(client.getAttributes()).orElse(new HashMap<>());
            attr.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, backchannelTokenDeliveryMode);
            client.setAttributes(attr);
        }
        String backchannelClientNotificationEndpoint = clientOIDC.getBackchannelClientNotificationEndpoint();
        if (backchannelClientNotificationEndpoint != null) {
            Map<String, String> attr = Optional.ofNullable(client.getAttributes()).orElse(new HashMap<>());
            attr.put(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, backchannelClientNotificationEndpoint);
            client.setAttributes(attr);
        }
        String backchannelAuthenticationRequestSigningAlg = clientOIDC.getBackchannelAuthenticationRequestSigningAlg();
        if (backchannelAuthenticationRequestSigningAlg != null) {
            Map<String, String> attr = Optional.ofNullable(client.getAttributes()).orElse(new HashMap<>());
            attr.put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, backchannelAuthenticationRequestSigningAlg);
            client.setAttributes(attr);
        }

        // PAR
        Boolean requirePushedAuthorizationRequests = clientOIDC.getRequirePushedAuthorizationRequests();
        if (requirePushedAuthorizationRequests != null) {
            Map<String, String> attr = Optional.ofNullable(client.getAttributes()).orElse(new HashMap<>());
            attr.put(ParConfig.REQUIRE_PUSHED_AUTHORIZATION_REQUESTS, requirePushedAuthorizationRequests.toString());
            client.setAttributes(attr);
        }

        configWrapper.setFrontChannelLogoutUrl(Optional.ofNullable(clientOIDC.getFrontChannelLogoutUri()).orElse(null));
        if (clientOIDC.getFrontchannelLogoutSessionRequired() == null) {
            // False by default per OIDC FrontChannel Logout specification
            configWrapper.setFrontChannelLogoutSessionRequired(false);
        } else {
            configWrapper.setFrontChannelLogoutSessionRequired(clientOIDC.getFrontchannelLogoutSessionRequired());
        }

        if (clientOIDC.getDefaultAcrValues() != null) {
            configWrapper.setAttributeMultivalued(Constants.DEFAULT_ACR_VALUES, clientOIDC.getDefaultAcrValues());
        }

        return client;
    }

    private static void setOidcCibaGrantEnabled(ClientRepresentation client, Boolean isEnabled) {
        if (isEnabled == null) return;
        Map<String, String> attributes = Optional.ofNullable(client.getAttributes()).orElse(new HashMap<>());
        attributes.put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, isEnabled.toString());
        client.setAttributes(attributes);
    }

    private static List<String> getSupportedAlgorithms(KeycloakSession session, Class<? extends Provider> clazz, boolean includeNone) {
        Stream<String> supportedAlgorithms = session.getKeycloakSessionFactory().getProviderFactoriesStream(clazz)
                .map(ProviderFactory::getId);

        if (includeNone) {
            supportedAlgorithms = Streams.concat(supportedAlgorithms, Stream.of("none"));
        }
        return supportedAlgorithms.collect(Collectors.toList());
    }

    private static boolean setPublicKey(OIDCClientRepresentation clientOIDC, ClientRepresentation clientRep) {
        OIDCAdvancedConfigWrapper configWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);

        if (clientOIDC.getJwks() != null) {
            if (clientOIDC.getJwksUri() != null) {
                throw new ClientRegistrationException("Illegal to use both jwks_uri and jwks");
            }

            JSONWebKeySet keySet = clientOIDC.getJwks();
            JWK publicKeyJWk = JWKSUtils.getKeyForUse(keySet, JWK.Use.SIG);

            try {
                configWrapper.setJwksString(JsonSerialization.writeValueAsPrettyString(clientOIDC.getJwks()));
            } catch (IOException e) {
                throw new ClientRegistrationException("Illegal jwks format");
            }
            configWrapper.setUseJwksString(true);
            configWrapper.setUseJwksUrl(false);

            if (publicKeyJWk == null) {
                return false;
            }
            PublicKey publicKey = JWKParser.create(publicKeyJWk).toPublicKey();
            String publicKeyPem = KeycloakModelUtils.getPemFromKey(publicKey);
            CertificateRepresentation rep = new CertificateRepresentation();
            rep.setPublicKey(publicKeyPem);
            rep.setKid(publicKeyJWk.getKeyId());
            CertificateInfoHelper.updateClientRepresentationCertificateInfo(clientRep, rep, JWTClientAuthenticator.ATTR_PREFIX);

            return true;
        } else if (clientOIDC.getJwksUri() != null) {
            configWrapper.setUseJwksUrl(true);
            configWrapper.setJwksUrl(clientOIDC.getJwksUri());
            configWrapper.setUseJwksString(false);
            return true;
        }

        return false;

    }

    public static OIDCClientRepresentation toExternalResponse(KeycloakSession session, ClientRepresentation client, URI uri) {
        OIDCClientRepresentation response = new OIDCClientRepresentation();
        response.setClientId(client.getClientId());

        if ("none".equals(client.getClientAuthenticatorType())) {
            response.setTokenEndpointAuthMethod("none");
        } else {
            ClientAuthenticatorFactory clientAuth = (ClientAuthenticatorFactory) session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, client.getClientAuthenticatorType());
            Set<String> oidcClientAuthMethods = clientAuth.getProtocolAuthenticatorMethods(OIDCLoginProtocol.LOGIN_PROTOCOL);
            if (oidcClientAuthMethods != null && !oidcClientAuthMethods.isEmpty()) {
                response.setTokenEndpointAuthMethod(oidcClientAuthMethods.iterator().next());
            }

            if (clientAuth.supportsSecret()) {
                response.setClientSecret(client.getSecret());
                response.setClientSecretExpiresAt(
                    OIDCClientSecretConfigWrapper.fromClientRepresentation(client).getClientSecretExpirationTime());
            }
        }

        response.setClientName(client.getName());
        response.setClientUri(client.getBaseUrl());
        response.setRedirectUris(client.getRedirectUris());
        response.setRegistrationAccessToken(client.getRegistrationAccessToken());
        response.setRegistrationClientUri(uri.toString());
        response.setResponseTypes(getOIDCResponseTypes(client));
        response.setGrantTypes(getOIDCGrantTypes(client));

        List<String> scopes = client.getOptionalClientScopes();
        if (scopes != null) response.setScope(scopes.stream().collect(Collectors.joining(" ")));

        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        if (config.isUserInfoSignatureRequired()) {
            response.setUserinfoSignedResponseAlg(config.getUserInfoSignedResponseAlg());
        }
        if (config.getUserInfoEncryptedResponseAlg() != null) {
            response.setUserinfoEncryptedResponseAlg(config.getUserInfoEncryptedResponseAlg());
        }
        if (config.getUserInfoEncryptedResponseEnc() != null) {
            response.setUserinfoEncryptedResponseEnc(config.getUserInfoEncryptedResponseEnc());
        }
        if (config.getRequestObjectSignatureAlg() != null) {
            response.setRequestObjectSigningAlg(config.getRequestObjectSignatureAlg());
        }
        if (config.getRequestObjectEncryptionAlg() != null) {
            response.setRequestObjectEncryptionAlg(config.getRequestObjectEncryptionAlg());
        }
        if (config.getRequestObjectEncryptionEnc() != null) {
            response.setRequestObjectEncryptionEnc(config.getRequestObjectEncryptionEnc());
        }
        if (config.isUseJwksUrl()) {
            response.setJwksUri(config.getJwksUrl());
        }
        if (config.isUseJwksString()) {
            try {
                response.setJwks(JsonSerialization.readValue(config.getJwksString(), JSONWebKeySet.class));
            } catch (IOException e) {
                throw new ClientRegistrationException("Illegal jwks format");
            }
        }
        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.5
        if (config.isUseMtlsHokToken()) {
            response.setTlsClientCertificateBoundAccessTokens(Boolean.TRUE);
        } else {
            response.setTlsClientCertificateBoundAccessTokens(Boolean.FALSE);
        }
        if (config.getTlsClientAuthSubjectDn() != null) {
            response.setTlsClientAuthSubjectDn(config.getTlsClientAuthSubjectDn());
        }
        if (config.getIdTokenSignedResponseAlg() != null) {
            response.setIdTokenSignedResponseAlg(config.getIdTokenSignedResponseAlg());
        }
        if (config.getIdTokenEncryptedResponseAlg() != null) {
            response.setIdTokenEncryptedResponseAlg(config.getIdTokenEncryptedResponseAlg());
        }
        if (config.getIdTokenEncryptedResponseEnc() != null) {
            response.setIdTokenEncryptedResponseEnc(config.getIdTokenEncryptedResponseEnc());
        }
        if (config.getAuthorizationSignedResponseAlg() != null) {
            response.setAuthorizationSignedResponseAlg(config.getAuthorizationSignedResponseAlg());
        }
        if (config.getAuthorizationEncryptedResponseAlg() != null) {
            response.setAuthorizationEncryptedResponseAlg(config.getAuthorizationEncryptedResponseAlg());
        }
        if (config.getAuthorizationEncryptedResponseEnc() != null) {
            response.setAuthorizationEncryptedResponseEnc(config.getAuthorizationEncryptedResponseEnc());
        }
        if (config.getRequestUris() != null) {
            response.setRequestUris(config.getRequestUris());
        }
        if (config.getTokenEndpointAuthSigningAlg() != null) {
            response.setTokenEndpointAuthSigningAlg(config.getTokenEndpointAuthSigningAlg());
        }
        if (config.getPostLogoutRedirectUris() != null) {
            response.setPostLogoutRedirectUris(config.getPostLogoutRedirectUris());
        }
        response.setBackchannelLogoutUri(config.getBackchannelLogoutUrl());
        response.setBackchannelLogoutSessionRequired(config.isBackchannelLogoutSessionRequired());
        response.setBackchannelLogoutSessionRequired(config.getBackchannelLogoutRevokeOfflineTokens());

        if (client.getAttributes() != null) {
            String mode = client.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT);
            if (StringUtil.isNotBlank(mode)) {
                response.setBackchannelTokenDeliveryMode(mode);
            }
            String clientNotificationEndpoint = client.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT);
            if (StringUtil.isNotBlank(clientNotificationEndpoint)) {
                response.setBackchannelClientNotificationEndpoint(clientNotificationEndpoint);
            }
            String alg = client.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG);
            if (StringUtil.isNotBlank(alg)) {
                response.setBackchannelAuthenticationRequestSigningAlg(alg);
            }
            Boolean requirePushedAuthorizationRequests = Boolean.valueOf(client.getAttributes().get(ParConfig.REQUIRE_PUSHED_AUTHORIZATION_REQUESTS));
            response.setRequirePushedAuthorizationRequests(requirePushedAuthorizationRequests.booleanValue());
        }

        List<ProtocolMapperRepresentation> foundPairwiseMappers = PairwiseSubMapperUtils.getPairwiseSubMappers(client);
        SubjectType subjectType = foundPairwiseMappers.isEmpty() ? SubjectType.PUBLIC : SubjectType.PAIRWISE;
        response.setSubjectType(subjectType.toString().toLowerCase());
        if (subjectType.equals(SubjectType.PAIRWISE)) {
            // Get sectorIdentifier from 1st found
            String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(foundPairwiseMappers.get(0));
            response.setSectorIdentifierUri(sectorIdentifierUri);
        }

        response.setFrontChannelLogoutUri(config.getFrontChannelLogoutUrl());
        response.setFrontchannelLogoutSessionRequired(config.isFrontChannelLogoutSessionRequired());

        List<String> defaultAcrValues = config.getAttributeMultivalued(Constants.DEFAULT_ACR_VALUES);
        if (!defaultAcrValues.isEmpty()) {
            response.setDefaultAcrValues(defaultAcrValues);
        }

        return response;
    }

    private static List<String> getOIDCResponseTypes(ClientRepresentation client) {
        List<String> responseTypes = new ArrayList<>();
        if (client.isStandardFlowEnabled()) {
            responseTypes.add(OAuth2Constants.CODE);
            responseTypes.add(OIDCResponseType.NONE);
        }
        if (client.isImplicitFlowEnabled()) {
            responseTypes.add(OIDCResponseType.ID_TOKEN);
            responseTypes.add("id_token token");
        }
        if (client.isStandardFlowEnabled() && client.isImplicitFlowEnabled()) {
            responseTypes.add("code id_token");
            responseTypes.add("code token");
            responseTypes.add("code id_token token");
        }
        return responseTypes;
    }

    private static List<String> getOIDCGrantTypes(ClientRepresentation client) {
        List<String> grantTypes = new ArrayList<>();
        if (client.isStandardFlowEnabled()) {
            grantTypes.add(OAuth2Constants.AUTHORIZATION_CODE);
        }
        if (client.isImplicitFlowEnabled()) {
            grantTypes.add(OAuth2Constants.IMPLICIT);
        }
        if (client.isDirectAccessGrantsEnabled()) {
            grantTypes.add(OAuth2Constants.PASSWORD);
        }
        if (client.isServiceAccountsEnabled()) {
            grantTypes.add(OAuth2Constants.CLIENT_CREDENTIALS);
        }
        boolean oauth2DeviceEnabled = client.getAttributes() != null && Boolean.parseBoolean(client.getAttributes().get(OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED));
        if (oauth2DeviceEnabled) {
            grantTypes.add(OAuth2Constants.DEVICE_CODE_GRANT_TYPE);
        }
        boolean oidcCibaEnabled = client.getAttributes() != null && Boolean.parseBoolean(client.getAttributes().get(OIDC_CIBA_GRANT_ENABLED));
        if (oidcCibaEnabled) {
            grantTypes.add(OAuth2Constants.CIBA_GRANT_TYPE);
        }
        if (client.getAuthorizationServicesEnabled() != null && client.getAuthorizationServicesEnabled()) {
            grantTypes.add(OAuth2Constants.UMA_GRANT_TYPE);
        }
        if (OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseRefreshToken()) {
            grantTypes.add(OAuth2Constants.REFRESH_TOKEN);
        }
        return grantTypes;
    }

}
