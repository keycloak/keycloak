package org.keycloak.tests.model;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientScopeEntity;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * Loading all client scopes of a realm with a cold cache must not issue queries per client scope.
 */
@KeycloakIntegrationTest
public class ClientScopeLoadQueryCountTest {

    private static final int CLIENT_SCOPES = 150;

    private static final String SCOPES_QUERY = "select scope from ClientScopeEntity scope where scope.realmId = :realm";

    // batches of 100 scopes, plus some slack for other scopes of the realm
    private static final long MAX_COLLECTION_FETCHES = 5;

    @InjectRealm(config = ClientScopesRealmConfig.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void loadingAllClientScopesDoesNotQueryPerScope() {
        String realmName = realm.getName();

        runOnServer.run(session -> session.getProvider(CacheRealmProvider.class).clear());

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            Statistics stats = session.getProvider(JpaConnectionProvider.class)
                    .getEntityManager()
                    .getEntityManagerFactory()
                    .unwrap(SessionFactory.class)
                    .getStatistics();
            boolean wasEnabled = stats.isStatisticsEnabled();
            stats.setStatisticsEnabled(true);
            stats.clear();
            long scopes;
            try {
                scopes = realmModel.getClientScopesStream().count();
            } finally {
                stats.setStatisticsEnabled(wasEnabled);
            }

            assertThat(scopes >= CLIENT_SCOPES, is(true));

            // only look at the statistics of the client scope entity, as the server runs other queries in parallel
            assertThat("scopes are loaded by a single query", stats.getQueryStatistics(SCOPES_QUERY).getExecutionCount(), is(1L));
            assertThat("no scope is loaded individually", stats.getEntityStatistics(ClientScopeEntity.class.getName()).getFetchCount(), is(0L));
            assertThat("attributes are batch-fetched", stats.getCollectionStatistics(ClientScopeEntity.class.getName() + ".attributes").getFetchCount(), lessThanOrEqualTo(MAX_COLLECTION_FETCHES));
            assertThat("scope mappings are batch-fetched", stats.getCollectionStatistics(ClientScopeEntity.class.getName() + ".scopeMappingIds").getFetchCount(), lessThanOrEqualTo(MAX_COLLECTION_FETCHES));
        });
    }

    private static class ClientScopesRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder r) {
            r.name("client-scope-load-realm");
            for (int i = 0; i < CLIENT_SCOPES; i++) {
                r.clientScopes(ClientScopeBuilder.create().name("scope-" + i).protocol("openid-connect").attribute("attr", "value-" + i));
            }
            return r;
        }
    }
}
