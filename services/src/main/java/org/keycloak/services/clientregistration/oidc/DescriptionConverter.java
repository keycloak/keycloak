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

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationException;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.util.JWKSUtils;

import java.net.URI;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

        List<String> oidcResponseTypes = clientOIDC.getResponseTypes();
        if (oidcResponseTypes == null || oidcResponseTypes.isEmpty()) {
            oidcResponseTypes = Collections.singletonList(OIDCResponseType.CODE);
        }
        List<String> oidcGrantTypes = clientOIDC.getGrantTypes();

        try {
            OIDCResponseType responseType = OIDCResponseType.parse(oidcResponseTypes);
            client.setStandardFlowEnabled(responseType.hasResponseType(OIDCResponseType.CODE));
            client.setImplicitFlowEnabled(responseType.isImplicitOrHybridFlow());

            client.setPublicClient(responseType.isImplicitFlow());

            if (oidcGrantTypes != null) {
                client.setDirectAccessGrantsEnabled(oidcGrantTypes.contains(OAuth2Constants.PASSWORD));
                client.setServiceAccountsEnabled(oidcGrantTypes.contains(OAuth2Constants.CLIENT_CREDENTIALS));
            }
        } catch (IllegalArgumentException iae) {
            throw new ClientRegistrationException(iae.getMessage(), iae);
        }

        String authMethod = clientOIDC.getTokenEndpointAuthMethod();
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

        boolean publicKeySet = setPublicKey(clientOIDC, client);
        if (authMethod != null && authMethod.equals(OIDCLoginProtocol.PRIVATE_KEY_JWT) && !publicKeySet) {
            throw new ClientRegistrationException("Didn't find key of supported keyType for use " + JWK.Use.SIG.asString());
        }

        OIDCAdvancedConfigWrapper configWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        if (clientOIDC.getUserinfoSignedResponseAlg() != null) {
            Algorithm algorithm = Enum.valueOf(Algorithm.class, clientOIDC.getUserinfoSignedResponseAlg());
            configWrapper.setUserInfoSignedResponseAlg(algorithm);
        }

        if (clientOIDC.getRequestObjectSigningAlg() != null) {
            Algorithm algorithm = Enum.valueOf(Algorithm.class, clientOIDC.getRequestObjectSigningAlg());
            configWrapper.setRequestObjectSignatureAlg(algorithm);
        }

        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.5
        Boolean tlsClientCertificateBoundAccessTokens = clientOIDC.getTlsClientCertificateBoundAccessTokens();
        if (tlsClientCertificateBoundAccessTokens != null) {
            if (tlsClientCertificateBoundAccessTokens.booleanValue()) configWrapper.setUseMtlsHoKToken(true);
            else configWrapper.setUseMtlsHoKToken(false);
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

        return client;
    }


    private static boolean setPublicKey(OIDCClientRepresentation clientOIDC, ClientRepresentation clientRep) {
        if (clientOIDC.getJwksUri() == null && clientOIDC.getJwks() == null) {
            return false;
        }

        if (clientOIDC.getJwksUri() != null && clientOIDC.getJwks() != null) {
            throw new ClientRegistrationException("Illegal to use both jwks_uri and jwks");
        }

        OIDCAdvancedConfigWrapper configWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);

        if (clientOIDC.getJwks() != null) {
            JSONWebKeySet keySet = clientOIDC.getJwks();
            JWK publicKeyJWk = JWKSUtils.getKeyForUse(keySet, JWK.Use.SIG);
            if (publicKeyJWk == null) {
                return false;
            } else {
                PublicKey publicKey = JWKParser.create(publicKeyJWk).toPublicKey();
                String publicKeyPem = KeycloakModelUtils.getPemFromKey(publicKey);
                CertificateRepresentation rep = new CertificateRepresentation();
                rep.setPublicKey(publicKeyPem);
                rep.setKid(publicKeyJWk.getKeyId());
                CertificateInfoHelper.updateClientRepresentationCertificateInfo(clientRep, rep, JWTClientAuthenticator.ATTR_PREFIX);

                configWrapper.setUseJwksUrl(false);

                return true;
            }
        } else {
            configWrapper.setUseJwksUrl(true);
            configWrapper.setJwksUrl(clientOIDC.getJwksUri());
            return true;
        }
    }


    public static OIDCClientRepresentation toExternalResponse(KeycloakSession session, ClientRepresentation client, URI uri) {
        OIDCClientRepresentation response = new OIDCClientRepresentation();
        response.setClientId(client.getClientId());

        ClientAuthenticatorFactory clientAuth = (ClientAuthenticatorFactory) session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, client.getClientAuthenticatorType());
        Set<String> oidcClientAuthMethods = clientAuth.getProtocolAuthenticatorMethods(OIDCLoginProtocol.LOGIN_PROTOCOL);
        if (oidcClientAuthMethods != null && !oidcClientAuthMethods.isEmpty()) {
            response.setTokenEndpointAuthMethod(oidcClientAuthMethods.iterator().next());
        }

        if (client.getClientAuthenticatorType().equals(ClientIdAndSecretAuthenticator.PROVIDER_ID)) {
            response.setClientSecret(client.getSecret());
            response.setClientSecretExpiresAt(0);
        }

        response.setClientName(client.getName());
        response.setClientUri(client.getBaseUrl());
        response.setRedirectUris(client.getRedirectUris());
        response.setRegistrationAccessToken(client.getRegistrationAccessToken());
        response.setRegistrationClientUri(uri.toString());
        response.setResponseTypes(getOIDCResponseTypes(client));
        response.setGrantTypes(getOIDCGrantTypes(client));

        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        if (config.isUserInfoSignatureRequired()) {
            response.setUserinfoSignedResponseAlg(config.getUserInfoSignedResponseAlg().toString());
        }
        if (config.getRequestObjectSignatureAlg() != null) {
            response.setRequestObjectSigningAlg(config.getRequestObjectSignatureAlg().toString());
        }
        if (config.isUseJwksUrl()) {
            response.setJwksUri(config.getJwksUrl());
        }
        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.5
        if (config.isUseMtlsHokToken()) {
            response.setTlsClientCertificateBoundAccessTokens(Boolean.TRUE);
        } else {
            response.setTlsClientCertificateBoundAccessTokens(Boolean.FALSE);
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

        List<ProtocolMapperRepresentation> foundPairwiseMappers = PairwiseSubMapperUtils.getPairwiseSubMappers(client);
        SubjectType subjectType = foundPairwiseMappers.isEmpty() ? SubjectType.PUBLIC : SubjectType.PAIRWISE;
        response.setSubjectType(subjectType.toString().toLowerCase());
        if (subjectType.equals(SubjectType.PAIRWISE)) {
            // Get sectorIdentifier from 1st found
            String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(foundPairwiseMappers.get(0));
            response.setSectorIdentifierUri(sectorIdentifierUri);
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
        if (client.getAuthorizationServicesEnabled() != null && client.getAuthorizationServicesEnabled()) {
            grantTypes.add(OAuth2Constants.UMA_GRANT_TYPE);
        }
        grantTypes.add(OAuth2Constants.REFRESH_TOKEN);
        return grantTypes;
    }

}
