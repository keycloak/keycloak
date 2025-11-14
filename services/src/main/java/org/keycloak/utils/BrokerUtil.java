/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.utils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.common.util.Base64Url;
import org.keycloak.representations.account.AccountLinkUriRepresentation;
import org.keycloak.services.Urls;

/**
 * TODO: Remove this class once support for "client initiated account linking" is removed (Probably Keycloak 27)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BrokerUtil {

    public static AccountLinkUriRepresentation createClientInitiatedLinkURI(String clientId, String redirectUri, String identityProviderAlias, String realmName, String userSessionId, URI serverBaseUri) {
        try {
            String nonce = UUID.randomUUID().toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = nonce + userSessionId + clientId + identityProviderAlias;
            byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = Base64Url.encode(check);
            URI linkUri = Urls.identityProviderLinkRequest(serverBaseUri, identityProviderAlias, realmName);
            linkUri = UriBuilder.fromUri(linkUri)
                    .queryParam("nonce", nonce)
                    .queryParam("hash", hash)
                    .queryParam("client_id", clientId)
                    .queryParam("redirect_uri", redirectUri)
                    .build();

            AccountLinkUriRepresentation rep = new AccountLinkUriRepresentation();
            rep.setAccountLinkUri(linkUri);
            rep.setHash(hash);
            rep.setNonce(nonce);
            return rep;
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }
}
