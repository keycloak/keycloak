package org.keycloak.services.trustchain;

import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.provider.Provider;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import java.util.List;
import java.util.Set;

public interface TrustChainProcessor extends Provider {

    List<TrustChainResolution> constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean policyRequired);
    List<TrustChainResolution> subTrustChains(String initialEntity, EntityStatement leafEs, Set<String> trustAnchorIds, Set<String> visitedNodes);
    EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException ;
    void validateToken(String token, JSONWebKeySet jwks);
    TrustChainResolution findAcceptableMetadataPolicyChain(List<TrustChainResolution> trustChainResolutions, EntityStatement statement);
    JSONWebKeySet getKeySet();
}
