package org.keycloak.tests.broker;

import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = SamlIdentityProviderStoreTokenV2Test.IdentityBrokeringAPIV2ServerConfig.class)
public class SamlIdentityProviderStoreTokenV2Test implements InterfaceIdentityProviderStoreTokenV2Test, InterfaceSamlIdentityProviderStoreTokenTest {

    @InjectRealm(config = IdpRealmConfig.class)
    protected ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectOAuthClient(config = ExternalClientConfig.class)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Override
    public ManagedRealm getRealm() {
        return realm;
    }

    @Override
    public ManagedRealm getExternalRealm() {
        return externalRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public RunOnServerClient getRunOnServer() {
        return runOnServer;
    }

    @Override
    public TimeOffSet getTimeOffSet() {
        return timeOffSet;
    }

    static class IdentityBrokeringAPIV2ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_BROKERING_API_V2);
        }
    }
}
