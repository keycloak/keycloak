package org.keycloak.tests.providers.broker.oidc;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

public class MissingUseJwksRealmResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public MissingUseJwksRealmResourceProvider(KeycloakSession session) {
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
        JWK[] jwks = session.keys().getKeysStream(realm)
                .filter(k -> k.getStatus().isEnabled() && k.getPublicKey() != null)
                .map(k -> {
                    JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithmOrDefault());
                    List<X509Certificate> certificates = Optional.ofNullable(k.getCertificateChain())
                        .filter(certs -> !certs.isEmpty())
                        .orElseGet(() -> Collections.singletonList(k.getCertificate()));
                    if (k.getType().equals(KeyType.RSA)) {
                        JWK rsaKey = b.rsa(k.getPublicKey(), certificates, k.getUse());
                        rsaKey.setPublicKeyUse(null);
                        return rsaKey;
                    } else if (k.getType().equals(KeyType.EC)) {
                        JWK ecKey = b.ec(k.getPublicKey(), k.getUse());
                        ecKey.setPublicKeyUse(null);
                        return ecKey;
                    } else if (k.getType().equals(KeyType.OKP)) {
                        JWK okpKey = b.okp(k.getPublicKey(), k.getUse());
                        okpKey.setPublicKeyUse(null);
                        return okpKey;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toArray(JWK[]::new);

        JSONWebKeySet keySet = new JSONWebKeySet();
        keySet.setKeys(jwks);

        return Response.ok(keySet).build();
    }

    @Override
    public void close() {
    }
}
