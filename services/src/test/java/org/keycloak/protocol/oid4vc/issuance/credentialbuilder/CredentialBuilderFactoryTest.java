package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

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

    @Test
    public void testVerifyNonNullConfigProperties() {
        List<CredentialBuilderFactory> credentialBuilderFactories = session
            .getKeycloakSessionFactory()
            .getProviderFactoriesStream(CredentialBuilder.class)
            .filter(CredentialBuilderFactory.class::isInstance)
            .map(CredentialBuilderFactory.class::cast)
            .toList();

        assertThat(credentialBuilderFactories, is(not(empty())));

        for (CredentialBuilderFactory credentialBuilderFactory : credentialBuilderFactories) {
            assertThat(credentialBuilderFactory.getConfigProperties(), notNullValue());
        }
    }
}
