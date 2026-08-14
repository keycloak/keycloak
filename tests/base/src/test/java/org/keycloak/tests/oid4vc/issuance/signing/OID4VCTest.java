package org.keycloak.tests.oid4vc.issuance.signing;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.tests.oid4vc.OID4VCIssuerEndpointTest;

/**
 * New-testsuite local utility base for SD-JWT signing/builder tests.
 */
public abstract class OID4VCTest extends OID4VCIssuerEndpointTest {

    protected static final String CONTEXT_URL = "https://www.w3.org/2018/credentials/v1";
    protected static final URI TEST_DID = URI.create("did:web:test.org");
    protected static final List<String> TEST_TYPES = List.of("VerifiableCredential");
    protected static final Instant TEST_EXPIRATION_DATE = Instant.now().plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

    protected static CredentialSubject getCredentialSubject(Map<String, Object> claims) {
        CredentialSubject credentialSubject = new CredentialSubject();
        claims.forEach(credentialSubject::setClaims);
        return credentialSubject;
    }

    protected static VerifiableCredential getTestCredential(Map<String, Object> claims) {
        VerifiableCredential credential = new VerifiableCredential();
        credential.setId(URI.create(String.format("uri:uuid:%s", UUID.randomUUID())));
        credential.setContext(List.of(CONTEXT_URL));
        credential.setType(TEST_TYPES);
        credential.setIssuer(TEST_DID);
        credential.setExpirationDate(TEST_EXPIRATION_DATE);
        Optional.ofNullable(claims.get("issuanceDate"))
                .filter(Instant.class::isInstance)
                .map(Instant.class::cast)
                .ifPresent(credential::setIssuanceDate);
        credential.setCredentialSubject(getCredentialSubject(claims));
        return credential;
    }
}
