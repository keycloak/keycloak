package org.keycloak.keys;

import org.keycloak.jose.jws.AlgorithmType;

import java.util.Collections;
import java.util.Map;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public interface EcdsaKeyProviderFactory extends KeyProviderFactory {

    @Override
    default Map<String, Object> getTypeMetadata() {
        return Collections.singletonMap("algorithmType", AlgorithmType.ECDSA);
    }

}
