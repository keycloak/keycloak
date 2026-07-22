package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CredentialOfferProviderTest {

    @Test
    public void shouldDelegateLongExpirationToIntegerSpiImplementation() {
        AtomicReference<Integer> delegatedExpiration = new AtomicReference<>();
        CredentialOfferProvider provider = (user, grantType, credentialConfigurationIds, targetClientId, targetUserId,
                                            expireAt) -> {
            delegatedExpiration.set(expireAt);
            return null;
        };

        provider.createCredentialOffer(null, null, null, null, null, 123L);

        assertEquals(Integer.valueOf(123), delegatedExpiration.get());
    }
}
