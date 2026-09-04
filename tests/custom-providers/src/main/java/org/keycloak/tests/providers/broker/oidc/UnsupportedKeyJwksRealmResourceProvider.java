package org.keycloak.tests.providers.broker.oidc;

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
import org.keycloak.services.resource.RealmResourceProvider;

public class UnsupportedKeyJwksRealmResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public UnsupportedKeyJwksRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
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

    @Override
    public void close() {
    }
}
