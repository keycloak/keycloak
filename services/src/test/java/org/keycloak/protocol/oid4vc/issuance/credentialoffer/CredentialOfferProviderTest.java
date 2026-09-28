package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.events.Errors;
import org.keycloak.protocol.oid4vc.issuance.CredentialOfferException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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

    @Test
    public void shouldRejectLongExpirationOutsideIntegerSpiRange() {
        CredentialOfferProvider provider = (user, grantType, credentialConfigurationIds, targetClientId, targetUserId,
                                            expireAt) -> null;
        long expireAt = (long) Integer.MAX_VALUE + 1;

        CredentialOfferException exception = assertThrows(CredentialOfferException.class,
                () -> provider.createCredentialOffer(null, null, null, null, null, expireAt));

        assertEquals(Errors.INVALID_REQUEST, exception.getErrorType());
        assertEquals("Credential offer expiration is outside the integer range supported by this provider: " + expireAt,
                exception.getMessage());
    }
}
