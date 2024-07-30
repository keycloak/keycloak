package org.keycloak.protocol.oid4vc.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * @author Pascal Knueppel
 * @since 02.07.2024
 */
public class VerifiableCredentialTest {

    @Test
    public void testIssuerIsDeserializedAsUri() throws IOException {
        final String verifiableCredentialJson = """
            {
              "@context": [
                "https://www.w3.org/ns/credentials/v2",
                "https://www.w3.org/ns/credentials/examples/v2"
              ],
              "id": "http://university.example/credentials/3732",
              "type": ["VerifiableCredential", "ExampleDegreeCredential"],
              "issuer": "https://university.example/issuers/565049",
              "validFrom": "2010-01-01T00:00:00Z",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "type": "ExampleBachelorDegree",
                  "name": "Bachelor of Science and Arts"
                }
              }
            }
            """;
        VerifiableCredential verifiableCredential = JsonSerialization.readValue(verifiableCredentialJson,
            VerifiableCredential.class);
        Assert.assertEquals(URI.class, verifiableCredential.getIssuer().getClass());
    }

    @Test
    public void testDeserializeIssuerAsMap() throws IOException {
        final String verifiableCredentialJson = """
            {
              "@context": [
                "https://www.w3.org/ns/credentials/v2",
                "https://www.w3.org/ns/credentials/examples/v2"
              ],
              "id": "http://university.example/credentials/3732",
              "type": ["VerifiableCredential", "ExampleDegreeCredential"],
              "issuer": {
                "id": "https://university.example/issuers/565049",
                "name": "Example University",
                "description": "A public university focusing on teaching examples."
              },
              "validFrom": "2015-05-10T12:30:00Z",
              "name": "Example University Degree",
              "description": "2015 Bachelor of Science and Arts Degree",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "type": "ExampleBachelorDegree",
                  "name": "Bachelor of Science and Arts"
                }
              }
            }
            """;
        VerifiableCredential verifiableCredential = JsonSerialization.readValue(verifiableCredentialJson,
            VerifiableCredential.class);
        Assert.assertTrue(Map.class.isAssignableFrom(verifiableCredential.getIssuer().getClass()));
    }

}
