package org.keycloak.protocol.oid4vc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.SupportedCredential;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class OID4VCClientRegistrationProviderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "Single Supported Credential with format, single-type and expiry.",
                        Map.of(
                                "vc.credential-id.expiry_in_s", "100",
                                "vc.credential-id.format", Format.JWT_VC.toString(),
                                "vc.credential-id.scope", "VerifiableCredential"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredential()
                                        .setId("credential-id")
                                        .setFormat(Format.JWT_VC)
                                        .setExpiryInSeconds(100l)
                                        .setScope("VerifiableCredential")),
                                null, null)
                },
                {
                        "Single Supported Credential with format, multi-type and expiry.",
                        Map.of(
                                "vc.credential-id.expiry_in_s", "100",
                                "vc.credential-id.format", Format.JWT_VC.toString(),
                                "vc.credential-id.scope", "AnotherCredential"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredential()
                                        .setId("credential-id")
                                        .setFormat(Format.JWT_VC)
                                        .setExpiryInSeconds(100l)
                                        .setScope("AnotherCredential")),
                                null, null)
                },
                {
                        "Single Supported Credential with format, multi-type and no expiry.",
                        Map.of(
                                "vc.credential-id.format", Format.JWT_VC.toString(),
                                "vc.credential-id.scope", "AnotherCredential"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredential()
                                        .setId("credential-id")
                                        .setFormat(Format.JWT_VC)
                                        .setScope("AnotherCredential")),
                                null, null)
                },
                {
                        "Single Supported Credential with format, multi-type, no expiry and a display object.",
                        Map.of(
                                "vc.credential-id.format", Format.JWT_VC.toString(),
                                "vc.credential-id.scope", "AnotherCredential",
                                "vc.credential-id.display.name", "Another",
                                "vc.credential-id.display.locale", "en"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredential()
                                        .setId("credential-id")
                                        .setFormat(Format.JWT_VC)
                                        .setDisplay(new DisplayObject().setLocale("en").setName("Another"))
                                        .setScope("AnotherCredential")),
                                null, null)
                },
                {
                        "Multiple Supported Credentials.",
                        Map.of(
                                "vc.first-id.expiry_in_s", "100",
                                "vc.first-id.format", Format.JWT_VC.toString(),
                                "vc.first-id.scope", "AnotherCredential",
                                "vc.first-id.display.name", "First",
                                "vc.first-id.display.locale", "en",
                                "vc.second-id.format", Format.SD_JWT_VC.toString(),
                                "vc.second-id.scope", "MyType",
                                "vc.second-id.display.name", "Second Credential",
                                "vc.second-id.display.locale", "de"),
                        new OID4VCClient(null, "did:web:test.org",
                                List.of(new SupportedCredential()
                                                .setId("first-id")
                                                .setFormat(Format.JWT_VC)
                                                .setExpiryInSeconds(100l)
                                                .setDisplay(new DisplayObject().setLocale("en").setName("First"))
                                                .setScope("AnotherCredential"),
                                        new SupportedCredential()
                                                .setId("second-id")
                                                .setFormat(Format.SD_JWT_VC)
                                                .setDisplay(new DisplayObject().setLocale("de").setName("Second Credential"))
                                                .setScope("MyType")),
                                null, null)
                }
        });
    }

    private Map<String, String> clientAttributes;
    private OID4VCClient oid4VCClient;

    public OID4VCClientRegistrationProviderTest(String name, Map<String, String> clientAttributes, OID4VCClient oid4VCClient) {
        this.clientAttributes = clientAttributes;
        this.oid4VCClient = oid4VCClient;
    }

    @Test
    public void testToClientRepresentation() {
        Map<String, String> translatedAttributes = OID4VCClientRegistrationProvider.toClientRepresentation(oid4VCClient).getAttributes();

        assertEquals("The client should have been translated into the correct clientRepresentation.", clientAttributes.entrySet().size(), translatedAttributes.size());
        clientAttributes.forEach((key, value) ->
                assertEquals("The client should have been translated into the correct clientRepresentation.", clientAttributes.get(key), translatedAttributes.get(key)));
    }

    @Test
    public void testFromClientAttributes() {
        assertEquals("The client should have been correctly build from the client representation",
                oid4VCClient,
                OID4VCClientRegistrationProvider.fromClientAttributes("did:web:test.org", clientAttributes));
    }

}