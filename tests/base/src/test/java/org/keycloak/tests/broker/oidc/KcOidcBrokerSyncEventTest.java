package org.keycloak.tests.broker.oidc;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractBrokerSyncEventTest;
import org.keycloak.testsuite.util.ProtocolMapperUtil;

@KeycloakIntegrationTest
public class KcOidcBrokerSyncEventTest extends AbstractBrokerSyncEventTest {

    private static final String IDP_ALIAS = "kc-oidc-idp";
    private static final String CLIENT_ID = "broker-client";
    private static final String CLIENT_SECRET = "broker-secret";

    @InjectRealm(ref = PROVIDER_REALM, config = ProviderRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = CONSUMER_REALM, config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @Override
    protected ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    protected ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    protected String getIdpAlias() {
        return IDP_ALIAS;
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(PROVIDER_REALM)
                    .clients(ClientBuilder.create(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("*")
                            .protocolMappers(ProtocolMapperUtil.createClaimMapper(DEPARTMENT, DEPARTMENT, DEPARTMENT,
                                    "String", true, true, true, false)))
                    .users(createProviderUser());
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            String providerBase = SERVER_ROOT + "/realms/" + PROVIDER_REALM;

            return realm.name(CONSUMER_REALM)
                    .identityProviders(IdentityProviderBuilder.create()
                            .alias(IDP_ALIAS)
                            .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                            .enabled(true)
                            .trustEmail(true)
                            .attribute("clientId", CLIENT_ID)
                            .attribute("clientSecret", CLIENT_SECRET)
                            .attribute("authorizationUrl", providerBase + "/protocol/openid-connect/auth")
                            .attribute("tokenUrl", providerBase + "/protocol/openid-connect/token")
                            .attribute("userInfoUrl", providerBase + "/protocol/openid-connect/userinfo")
                            .attribute("jwksUrl", providerBase + "/protocol/openid-connect/certs")
                            .attribute("defaultScope", "openid email profile")
                            .attribute(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.name()))
                    .identityProviderMappers(
                            createIdpMapper(IDP_ALIAS, DEPARTMENT, UserAttributeMapper.PROVIDER_ID)
                                    .attribute(AbstractClaimMapper.CLAIM, DEPARTMENT)
                                    .attribute(UserAttributeMapper.USER_ATTRIBUTE, DEPARTMENT));
        }
    }
}
