package org.keycloak.tests.admin.identityprovider;

import java.util.List;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderConfig;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderFactory;
import org.keycloak.models.IdentityProviderCapability;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.client.authentication.external.SpiffeClientAuthTest;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = SpiffeClientAuthTest.SpiffeServerConfig.class)
public class IdentityProviderTypeTest {

    @InjectRealm(config = MyRealm.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testFilterByType() {
        MatcherAssert.assertThat(getIdps(null, null), Matchers.containsInAnyOrder("myoidc", "myspiffe", "mysaml"));
        MatcherAssert.assertThat(getIdps(IdentityProviderType.ANY, null), Matchers.containsInAnyOrder("myoidc", "myspiffe", "mysaml"));
        MatcherAssert.assertThat(getIdps(IdentityProviderType.USER_AUTHENTICATION, null), Matchers.containsInAnyOrder("myoidc", "mysaml"));
        MatcherAssert.assertThat(getIdps(IdentityProviderType.CLIENT_ASSERTION, null), Matchers.containsInAnyOrder("myoidc", "myspiffe"));
    }

    @Test
    public void testFilterByCapability() {
        MatcherAssert.assertThat(getIdps(null, IdentityProviderCapability.USER_LINKING), Matchers.containsInAnyOrder("myoidc", "mysaml"));
    }

    @Test
    public void testDefaultsToUserAuthenticationProviders() {
        runOnServer.run(s -> {
            List<String> idps = s.identityProviders().getAllStream().map(IdentityProviderModel::getAlias).toList();
            MatcherAssert.assertThat(idps, Matchers.containsInAnyOrder("myoidc", "mysaml"));
        });
    }

    private List<String> getIdps(IdentityProviderType type, IdentityProviderCapability capability) {
        return realm.admin().identityProviders()
                .find(type != null ? type.name() : null, capability != null ? capability.name() : null, null, null, 0, 100)
                .stream().map(IdentityProviderRepresentation::getAlias).toList();
    }

    public static class MyRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm
                    .identityProvider(IdentityProviderBuilder.create()
                        .providerId(SpiffeIdentityProviderFactory.PROVIDER_ID)
                        .alias("myspiffe")
                        .setAttribute(SpiffeIdentityProviderConfig.TRUST_DOMAIN_KEY, "spiffe://mytrust")
                        .setAttribute(SpiffeIdentityProviderConfig.BUNDLE_ENDPOINT_KEY, "https://myendpoint")
                        .build())
                    .identityProvider(IdentityProviderBuilder.create()
                        .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                        .alias("myoidc")
                        .build())
                    .identityProvider(IdentityProviderBuilder.create()
                        .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                        .alias("mysaml")
                        .build());
        }
    }

}
