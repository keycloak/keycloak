package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import java.util.List;

import org.keycloak.VCFormat;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSigner;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class CredentialBuilderFactoryTest {

    private static KeycloakSession session;

    @BeforeClass
    public static void beforeClass() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver(Feature.OID4VC_VCI.getVersionedKey(), ""));
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        ResteasyKeycloakSessionFactory factory = new ResteasyKeycloakSessionFactory();
        factory.init();
        session = new ResteasyKeycloakSession(factory);
    }

    @AfterClass
    public static void afterClass() {
        Profile.reset();
    }

    @Test
    public void testVerifyNonNullConfigProperties() {
        List<CredentialBuilderFactory> credentialBuilderFactories = session
            .getKeycloakSessionFactory()
            .getProviderFactoriesStream(CredentialBuilder.class)
            .filter(CredentialBuilderFactory.class::isInstance)
            .map(CredentialBuilderFactory.class::cast)
            .toList();

        assertFalse(credentialBuilderFactories.isEmpty());

        for (CredentialBuilderFactory credentialBuilderFactory : credentialBuilderFactories) {
            assertNotNull(credentialBuilderFactory.getConfigProperties());
        }
    }

    @Test
    public void testLdpFactoriesDisabled() {
        // LDP providers are intentionally disabled to keep scope limited to supported formats.
        List<String> builderIds = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialBuilder.class)
                .map(f -> f.getId())
                .toList();

        assertFalse(builderIds.contains("ldp_vc"));

        List<String> signerIds = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialSigner.class)
                .map(f -> f.getId())
                .toList();

        assertFalse(signerIds.contains("ldp_vc"));
    }

    @Test
    public void testMdocFactoriesDisabledWithoutMdocFeature() {
        List<String> builderIds = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialBuilder.class)
                .map(f -> f.getId())
                .toList();

        assertFalse(builderIds.contains(VCFormat.MSO_MDOC));

        List<String> signerIds = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CredentialSigner.class)
                .map(f -> f.getId())
                .toList();

        assertFalse(signerIds.contains(VCFormat.MSO_MDOC));
    }
}
