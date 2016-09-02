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

package org.keycloak.testsuite.rest.resource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.keycloak.OAuth2Constants;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testsuite.rest.TestApplicationResourceProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestingOIDCEndpointsApplicationResource {

    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";

    private final TestApplicationResourceProviderFactory.OIDCClientData clientData;

    public TestingOIDCEndpointsApplicationResource(TestApplicationResourceProviderFactory.OIDCClientData oidcClientData) {
        this.clientData = oidcClientData;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generate-keys")
    @NoCache
    public Map<String, String> generateKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            clientData.setSigningKeyPair(generator.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            throw new BadRequestException("Error generating signing keypair", e);
        }

        String privateKeyPem = KeycloakModelUtils.getPemFromKey(clientData.getSigningKeyPair().getPrivate());
        String publicKeyPem = KeycloakModelUtils.getPemFromKey(clientData.getSigningKeyPair().getPublic());

        Map<String, String> res = new HashMap<>();
        res.put(PRIVATE_KEY, privateKeyPem);
        res.put(PUBLIC_KEY, publicKeyPem);
        return res;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/get-jwks")
    @NoCache
    public JSONWebKeySet getJwks() {
        JSONWebKeySet keySet = new JSONWebKeySet();

        if (clientData.getSigningKeyPair() == null) {
            keySet.setKeys(new JWK[] {});
        } else {
            keySet.setKeys(new JWK[] { JWKBuilder.create().rs256(clientData.getSigningKeyPair().getPublic()) });
        }

        return keySet;
    }


    @GET
    @Path("/set-oidc-request")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JWT)
    @NoCache
    public void setOIDCRequest(@QueryParam("realmName") String realmName, @QueryParam("clientId") String clientId,
                               @QueryParam("redirectUri") String redirectUri, @QueryParam("maxAge") String maxAge,
                               @QueryParam("jwaAlgorithm") String jwaAlgorithm) {
        Map<String, Object> oidcRequest = new HashMap<>();
        oidcRequest.put(OIDCLoginProtocol.CLIENT_ID_PARAM, clientId);
        oidcRequest.put(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
        oidcRequest.put(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        if (maxAge != null) {
            oidcRequest.put(OIDCLoginProtocol.MAX_AGE_PARAM, Integer.parseInt(maxAge));
        }

        Algorithm alg = Enum.valueOf(Algorithm.class, jwaAlgorithm);
        if (alg == Algorithm.none) {
            clientData.setOidcRequest(new JWSBuilder().jsonContent(oidcRequest).none());
        } else if (alg == Algorithm.RS256) {
            if (clientData.getSigningKeyPair() == null) {
                throw new BadRequestException("Requested RS256, but signing key not set");
            }

            PrivateKey privateKey = clientData.getSigningKeyPair().getPrivate();
            clientData.setOidcRequest(new JWSBuilder().jsonContent(oidcRequest).rsa256(privateKey));
        } else {
            throw new BadRequestException("Unknown argument: " + jwaAlgorithm);
        }
    }


    @GET
    @Path("/get-oidc-request")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JWT)
    @NoCache
    public String getOIDCRequest() {
        return clientData.getOidcRequest();
    }
}
