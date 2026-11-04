package org.keycloak.tests.model;

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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Looking up a client scope by name must neither load all client scopes of the realm nor return stale results.
 */
@KeycloakIntegrationTest
public class ClientScopeLookupByNameTest {

    private static final int CLIENT_SCOPES = 150;

    private static final String SCOPES_QUERY = "select scope from ClientScopeEntity scope where scope.realmId = :realm";

    private static final String SCOPE_ID_BY_NAME_QUERY = "select scope.id from ClientScopeEntity scope where scope.realmId = :realm and scope.name = :name";

    @InjectRealm(config = ClientScopesRealmConfig.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void lookupByNameLoadsSingleClientScopeAndIsCached() {
        String realmName = realm.getName();

        runOnServer.run(session -> session.getProvider(CacheRealmProvider.class).clear());

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            Statistics stats = statistics(session);
            stats.clear();

            ClientScopeModel scope = realmModel.getClientScopeByName("scope-42");
            assertThat(scope, notNullValue());
            assertThat(scope.getName(), is("scope-42"));

            // only look at the statistics of the client scope entity, as the server runs other queries in parallel
            assertThat("scope is found by name", stats.getQueryStatistics(SCOPE_ID_BY_NAME_QUERY).getExecutionCount(), is(1L));
            assertThat("scopes are not enumerated", stats.getQueryStatistics(SCOPES_QUERY).getExecutionCount(), is(0L));
            assertThat("only one scope is loaded", stats.getEntityStatistics(ClientScopeEntity.class.getName()).getFetchCount(), lessThanOrEqualTo(1L));
        });

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            Statistics stats = statistics(session);
            stats.clear();

            assertThat(realmModel.getClientScopeByName("scope-42"), notNullValue());
            assertThat("second lookup is served from the cache", stats.getQueryStatistics(SCOPE_ID_BY_NAME_QUERY).getExecutionCount(), is(0L));
        });
    }

    @Test
    public void lookupByNameReflectsRenameAndRemoval() {
        String realmName = realm.getName();

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            assertThat(realmModel.getClientScopeByName("scope-1"), notNullValue());
            assertThat(realmModel.getClientScopeByName("scope-2"), notNullValue());
            assertThat(realmModel.getClientScopeByName("does-not-exist"), nullValue());
        });

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            realmModel.getClientScopeByName("scope-1").setName("scope-1-renamed");
            realmModel.removeClientScope(realmModel.getClientScopeByName("scope-2").getId());
        });

        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            assertThat(realmModel.getClientScopeByName("scope-1"), nullValue());
            assertThat(realmModel.getClientScopeByName("scope-1-renamed"), notNullValue());
            assertThat(realmModel.getClientScopeByName("scope-2"), nullValue());
        });
    }

    private static Statistics statistics(org.keycloak.models.KeycloakSession session) {
        Statistics stats = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        return stats;
    }

    private static class ClientScopesRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder r) {
            r.name("client-scope-lookup-realm");
            for (int i = 0; i < CLIENT_SCOPES; i++) {
                r.clientScopes(ClientScopeBuilder.create().name("scope-" + i).protocol("openid-connect"));
            }
            return r;
        }
    }
}
