/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker.oidc;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.crypto.KeyType;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class UnsupportedKeyJwksRestResource {

    private final KeycloakSession session;

    public UnsupportedKeyJwksRestResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("jwks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jwks() {
        RealmModel realm = session.getContext().getRealm();
        List<JWK> jwks = session.keys().getKeysStream(realm)
                .filter(k -> k.getStatus().isEnabled() && k.getPublicKey() != null)
                .map(k -> {
                    JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithmOrDefault());
                    List<X509Certificate> certificates = Optional.ofNullable(k.getCertificateChain())
                        .filter(certs -> !certs.isEmpty())
                        .orElseGet(() -> Collections.singletonList(k.getCertificate()));
                    if (k.getType().equals(KeyType.RSA)) {
                        return b.rsa(k.getPublicKey(), certificates, k.getUse());
                    } else if (k.getType().equals(KeyType.EC)) {
                        return b.ec(k.getPublicKey(), k.getUse());
                    } else if (k.getType().equals(KeyType.OKP)) {
                        return b.okp(k.getPublicKey(), k.getUse());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        // Add unsupported jwk
        JWK unsupported = new JWK();
        unsupported.setKeyType("EC");
        unsupported.setOtherClaims("crv", "unsupportedsecp256k1");
        unsupported.setKeyId("kf9t2WAuldbXS-e12bQf5anGnmxM4U5tfW0YaI1CqWQ");
        unsupported.setOtherClaims("x", "6nr5nOtISf6qopXw2YbjrlbJ7ZzPqtIAXjoibtq3PLk");
        unsupported.setOtherClaims("y", "Ct4wJp2CMSmL-eFBtIXowpJgw4Pn7HdR27laqI4zj14");
        jwks.add(unsupported);

        JSONWebKeySet keySet = new JSONWebKeySet();
        keySet.setKeys(jwks.toArray(JWK[]::new));

        return Response.ok(keySet).build();
    }
}
