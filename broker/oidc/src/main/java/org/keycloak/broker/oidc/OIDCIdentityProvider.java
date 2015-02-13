/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.broker.oidc;

import org.apache.commons.collections4.CollectionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.keycloak.broker.oidc.util.SimpleHttp;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.AuthenticationResponse;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.util.MultivaluedHashMap;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.*;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProvider extends AbstractOAuth2IdentityProvider<OIDCIdentityProviderConfig> {

    public static final String OAUTH2_PARAMETER_PROMPT = "prompt";
    public static final String OIDC_PARAMETER_ID_TOKEN = "id_token";

    public OIDCIdentityProvider(OIDCIdentityProviderConfig config) {
        super(config);
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        return super.createAuthorizationUrl(request)
                .queryParam(OAUTH2_PARAMETER_PROMPT, getConfig().getPrompt());
    }

    @Override
    protected AuthenticationResponse doHandleResponse(String response) throws IOException {
        String accessToken = extractTokenFromResponse(response, OAUTH2_PARAMETER_ACCESS_TOKEN);

        if (accessToken == null) {
            throw new RuntimeException("No access_token from server.");
        }

        String idToken = extractTokenFromResponse(response, OIDC_PARAMETER_ID_TOKEN);

        validateIdToken(idToken);

        try {
            JsonNode userInfo = SimpleHttp.doGet(getConfig().getUserInfoUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .asJson();

            String id = getJsonProperty(userInfo, "sub");
            String name = getJsonProperty(userInfo, "name");
            String preferredUsername = getJsonProperty(userInfo, "preferred_username");
            String email = getJsonProperty(userInfo, "email");

            FederatedIdentity identity = new FederatedIdentity(id);

            identity.setId(id);
            identity.setName(name);
            identity.setEmail(email);

            if (preferredUsername == null) {
                preferredUsername = email;
            }

            if (preferredUsername == null) {
                preferredUsername = id;
            }

            identity.setUsername(preferredUsername);
            identity.setClaims(getClaims(userInfo));

            return AuthenticationResponse.end(identity);
        } catch (Exception e) {
            throw new RuntimeException("Could not fetch attributes from userinfo endpoint.", e);
        }
    }

    private MultivaluedHashMap<String, String> getClaims(JsonNode userInfo) {
        MultivaluedHashMap<String, String> claims = null;

        try {
            ObjectMapper mapper = new ObjectMapper();

            //Note does this need to be String, Object
            LinkedHashMap<String, Object> hm = mapper.readValue(userInfo, LinkedHashMap.class);

            if(hm.size() > 0) {
                claims = new MultivaluedHashMap<String, String>();
                appendClaims(claims, hm, "idp");
            }
        }
        catch(Exception e) {
            throw new RuntimeException("Could not fetch attributes from userinfo endpoint.", e);
        }

        claims.add("authenticatingIdp", getConfig().getId());

        return claims;
    }

    private void appendClaims(MultivaluedHashMap<String, String> claims, LinkedHashMap<String, Object> hm, String prefix) {
        for(Map.Entry<String, Object> entry : hm.entrySet()) {
            //Todo: this is kind of a pain. I'm sure this doesn't cover everything.
            if(entry.getValue() instanceof LinkedHashMap) {
                appendClaims(claims, (LinkedHashMap<String, Object>) entry.getValue(), prefix + "." + entry.getKey());
            }
            else if(entry.getValue() instanceof ArrayList && ((ArrayList<?>)entry.getValue()).get(0) instanceof LinkedHashMap) {
                for(Object obj : (ArrayList)entry.getValue()) {
                    appendClaims(claims, (LinkedHashMap<String, Object>) obj, prefix + "." + entry.getKey());
                }
            }
            else {
                claims.add(prefix + "." + entry.getKey(), entry.getValue().toString());
            }
        }
    }

    /*private void appendClaims(MultivaluedHashMap<String, Object> claims, Map.Entry<String, Object> entry, String prefix) {

    }*/

    //private void appendClaims(MultivaluedHashMap<String, Object> claims, LinkedHashMap<String, Object> hm, String prefix) {

    //}

    private void validateIdToken(String idToken) {
        if (idToken == null) {
            throw new RuntimeException("No id_token from server.");
        }

        try {
            JsonNode idTokenInfo = asJsonNode(decodeJWS(idToken));

            String aud = getJsonProperty(idTokenInfo, "aud");
            String iss = getJsonProperty(idTokenInfo, "iss");

            if (aud != null && !aud.equals(getConfig().getClientId())) {
                throw new RuntimeException("Wrong audience from id_token..");
            }

            String trustedIssuers = getConfig().getIssuer();

            if (trustedIssuers != null) {
                String[] issuers = trustedIssuers.split(",");

                for (String trustedIssuer : issuers) {
                    if (iss != null && iss.equals(trustedIssuer.trim())) {
                        return;
                    }
                }

                throw new RuntimeException("Wrong issuer from id_token..");
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not decode id token.", e);
        }
    }

    private String decodeJWS(String token) {
        return new JWSInput(token).readContentAsString();
    }
}
