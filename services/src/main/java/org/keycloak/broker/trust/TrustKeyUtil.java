package org.keycloak.broker.trust;

import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.broker.provider.TrustMaterialRequest;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.util.Strings;

public class TrustKeyUtil {

    private TrustKeyUtil() {
    }

    public static Stream<JWK> filterKeys(Stream<JWK> keys, TrustMaterialRequest request) {
        return keys
                .filter(Objects::nonNull)
                .filter(key -> Strings.isEmpty(request.getKid()) || Objects.equals(request.getKid(), key.getKeyId()))
                .filter(key -> Strings.isEmpty(request.getAlgorithm()) || Strings.isEmpty(key.getAlgorithm())
                        || Objects.equals(request.getAlgorithm(), key.getAlgorithm()))
                .filter(key -> Strings.isEmpty(key.getPublicKeyUse()) || Objects.equals(JWK.Use.SIG.asString(), key.getPublicKeyUse()));
    }
}
