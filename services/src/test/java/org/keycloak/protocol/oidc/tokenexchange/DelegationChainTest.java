package org.keycloak.protocol.oidc.tokenexchange;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.representations.IDToken.ACT;
import static org.keycloak.representations.JsonWebToken.SUBJECT;

class DelegationChainTest {

    @Test
    void matchesPriorClientWhenSubjectChanges() {
        List<DelegationActor> actors = DelegationChain.actors(Map.of(
                SUBJECT, "other-subject", CLIENT_ID, "other-client", ACT,
                Map.of(SUBJECT, "prior-pairwise-subject", CLIENT_ID, "actor-client")));

        Assertions.assertTrue(DelegationChain.containsActor(actors, "actor-user", "current-subject", "actor-client"));
        Assertions.assertFalse(DelegationChain.containsActor(actors, "actor-user", "current-subject", "new-client"));
        Assertions.assertFalse(DelegationChain.containsActor(actors, "actor-user", "current-subject", null));
    }

    @Test
    void matchesEitherSubjectWithoutClientId() {
        List<DelegationActor> actors = DelegationChain.actors(Map.of(SUBJECT, "prior-subject"));

        Assertions.assertTrue(DelegationChain.containsActor(actors, "prior-subject", "current-subject", null));
        Assertions.assertTrue(DelegationChain.containsActor(actors, "actor-user", "prior-subject", null));
        Assertions.assertFalse(DelegationChain.containsActor(actors, "actor-user", null, null));
    }
}
