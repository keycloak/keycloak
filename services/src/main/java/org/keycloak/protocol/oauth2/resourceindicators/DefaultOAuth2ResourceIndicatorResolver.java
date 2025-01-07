package org.keycloak.protocol.oauth2.resourceindicators;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.KeycloakSessionUtil;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class DefaultOAuth2ResourceIndicatorResolver implements OAuth2ResourceIndicatorResolver {

    @Override
    public ClientModel findClientByResourceIndicator(String resourceIndicator) {

        if (resourceIndicator == null) {
            return null;
        }

        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        URI resourceIndicatorUri = URI.create(resourceIndicator);
        if (resourceIndicatorUri.getScheme() == null) {
            // try lookup a client by clientid
            return session.getContext().getRealm().getClientByClientId(resourceIndicator);
        }

        // TODO implement efficient client lookup by resourceIndicator URI
        return null;
    }

    @Override
    public Set<String> narrowResourceIndicators(ClientModel client, Set<String> resourceIndicatorCandidates) {

        // TODO implement a sane default to check client specific resource definitions.
        Set<String> result = new HashSet<>(resourceIndicatorCandidates);
        // dummy implementation that allows all requested resource, except those starting with "bad-"
        result.removeIf(item -> item.startsWith("bad-"));

        return resourceIndicatorCandidates;
    }

}
