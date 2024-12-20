package org.keycloak.protocol.oauth2.resourceindicators;

import org.keycloak.models.ClientModel;

import java.util.HashSet;
import java.util.Set;

public class DefaultOAuth2ResourceIndicatorsProvider implements OAuth2ResourceIndicatorsProvider {

    @Override
    public Set<String> narrowResourceIndicators(ClientModel client, Set<String> resourceIndicatorCandidates) {

        // TODO implement a sane default to check client specific resource definitions.
        Set<String> result = new HashSet<>(resourceIndicatorCandidates);
        // dummy implementation that allows all requested resource, except those starting with "bad-"
        result.removeIf(item -> item.startsWith("bad-"));

        return resourceIndicatorCandidates;
    }

}
