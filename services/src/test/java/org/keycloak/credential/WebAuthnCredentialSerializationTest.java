package org.keycloak.credential;

import org.keycloak.common.util.Base64Url;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.AttestationStatement;
import com.webauthn4j.data.attestation.statement.COSEKeyType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WebAuthnCredentialSerializationTest {

    // Fixed fixtures in the CBOR storage format used with WebAuthn4J 0.30.3 and Jackson 2.
    private static final String ES256_KEY = "pQECAyYgASFYIGsX0fLhLEJH-Lzm5WOkQPJ3A32BLeszoPShOUXYmMKW"
            + "IlggT-NC4v4af5uO5-tKfA-eFivOM1drMV7Oy7ZAaDe_UfU";
    private static final String RS256_KEY = "pAEDAzkBACBYgLKBQ46GeZkPFwAO63kABYPdIpygB5NA1zE4s6OCbjrIM1G4"
            + "XKZSHme5LPN4bBI5vJj_JVFWhzDzABxCKd4Lul8bIPrO96H8dXbbfxaO2o_I9_NzBUBpfPhb4sU1aGzXhngt"
            + "FwQUIzfJRJfpSMDmuGaBETQEoaZU3o9J7Q6e00JtIUMBAAE";

    @ParameterizedTest
    @CsvSource({
            ES256_KEY + ", ES256, EC2",
            RS256_KEY + ", RS256, RSA"
    })
    public void readExistingPublicKey(String stored, String algorithm, COSEKeyType keyType) {
        CredentialPublicKeyConverter converter = new CredentialPublicKeyConverter(new ObjectConverter());
        COSEKey key = converter.convertToEntityAttribute(stored);
        assertEquals(algorithm, key.getAlgorithm().toString());
        assertEquals(keyType, key.getKeyType());
        assertNotNull(key.getPublicKey());

        COSEKey restored = converter.convertToEntityAttribute(converter.convertToDatabaseColumn(key));
        assertEquals(key, restored);
        assertArrayEquals(key.getPublicKey().getEncoded(), restored.getPublicKey().getEncoded());
    }

    @ParameterizedTest
    @CsvSource({
            "v2dhdHRTdG10v_9jZm10ZG5vbmX_, none",
            "v2dhdHRTdG10v2NhbGcmY3NpZ0MBAgP_Y2ZtdGZwYWNrZWT_, packed"
    })
    public void readExistingAttestationStatement(String stored, String format) {
        ObjectConverter objectConverter = new ObjectConverter();
        AttestationStatementConverter converter = new AttestationStatementConverter(objectConverter);
        AttestationStatement statement = converter.convertToEntityAttribute(stored);
        assertEquals(format, statement.getFormat());

        AttestationStatement restored = converter.convertToEntityAttribute(converter.convertToDatabaseColumn(statement));
        assertEquals(format, restored.getFormat());

        String roundTripped = converter.convertToDatabaseColumn(restored);
        assertEquals(
                objectConverter.getCborConverter().readTree(Base64Url.decode(stored)),
                objectConverter.getCborConverter().readTree(Base64Url.decode(roundTripped))
        );
    }
}
