package org.keycloak.tests.model;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.jpa.entities.ClientScopeEntity;
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
import org.infinispan.Cache;
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
            withStatistics(session, stats -> {
                long scopes = realmModel.getClientScopesStream().count();
                assertThat(scopes >= CLIENT_SCOPES, is(true));

                // only look at the statistics of the client scope entity, as the server runs other queries in parallel
                assertThat("scopes are loaded by a single query", stats.getQueryStatistics(SCOPES_QUERY).getExecutionCount(), is(1L));
                assertThat("no scope is loaded individually", stats.getEntityStatistics(ClientScopeEntity.class.getName()).getFetchCount(), is(0L));
                assertThat("attributes are batch-fetched", stats.getCollectionStatistics(ClientScopeEntity.class.getName() + ".attributes").getFetchCount(), lessThanOrEqualTo(MAX_COLLECTION_FETCHES));
                assertThat("scope mappings are batch-fetched", stats.getCollectionStatistics(ClientScopeEntity.class.getName() + ".scopeMappingIds").getFetchCount(), lessThanOrEqualTo(MAX_COLLECTION_FETCHES));
            });
        });
    }

    @Test
    public void fewEvictedClientScopesAreLoadedIndividually() {
        evictScopes(3);

        String realmName = realm.getName();
        runOnServer.run(session -> withStatistics(session, stats -> {
            session.realms().getRealmByName(realmName).getClientScopesStream().count();

            assertThat("no bulk query for a few missing scopes", stats.getQueryStatistics(SCOPES_QUERY).getExecutionCount(), is(0L));
            assertThat("only missing scopes are loaded", stats.getEntityStatistics(ClientScopeEntity.class.getName()).getLoadCount(), is(3L));
        }));
    }

    @Test
    public void manyEvictedClientScopesAreLoadedInBatches() {
        evictScopes(50);

        String realmName = realm.getName();
        runOnServer.run(session -> withStatistics(session, stats -> {
            session.realms().getRealmByName(realmName).getClientScopesStream().count();

            assertThat("missing scopes are loaded by a single query", stats.getQueryStatistics(SCOPES_QUERY).getExecutionCount(), is(1L));
            assertThat("no scope is loaded individually", stats.getEntityStatistics(ClientScopeEntity.class.getName()).getFetchCount(), is(0L));
            // batches only cover part of the missing scopes, but there must be far fewer fetches than missing scopes
            assertThat("attributes are batch-fetched", stats.getCollectionStatistics(ClientScopeEntity.class.getName() + ".attributes").getFetchCount(), lessThanOrEqualTo(15L));
        }));
    }

    // warms the cache, then removes the given number of client scopes from it, as the cache eviction would do
    private void evictScopes(int count) {
        String realmName = realm.getName();
        runOnServer.run(session -> {
            session.getProvider(CacheRealmProvider.class).clear();
            session.realms().getRealmByName(realmName).getClientScopesStream().count();
        });
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            Set<String> names = IntStream.range(0, count).mapToObj(i -> "scope-" + i).collect(Collectors.toSet());
            Cache<Object, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
            realmModel.getClientScopesStream().filter(s -> names.contains(s.getName())).map(ClientScopeModel::getId).toList().forEach(cache::remove);
        });
    }

    private static void withStatistics(org.keycloak.models.KeycloakSession session, Consumer<Statistics> test) {
        Statistics stats = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        boolean wasEnabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        try {
            stats.clear();
            test.accept(stats);
        } finally {
            stats.setStatisticsEnabled(wasEnabled);
        }
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
