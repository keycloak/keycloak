package org.keycloak.crypto;

import org.keycloak.jose.jwe.alg.JWEAlgorithmProvider;

public interface JWEAlgorithmProviderAccessor {

    JWEAlgorithmProvider getJweAlgorithmProvider(String name);
}
