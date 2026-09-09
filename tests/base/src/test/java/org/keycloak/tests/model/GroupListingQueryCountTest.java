package org.keycloak.tests.model;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import org.hibernate.Session;
import org.hibernate.SessionEventListener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
public class GroupListingQueryCountTest {

    private static final int GROUP_COUNT = 25;
    private static final int PAGE_SIZE = 20;

    @InjectRealm(config = GroupListingRealmConfig.class)
    ManagedRealm realm;

    @TestOnServer
    public void topLevelGroupsPageIsHydratedByASingleQuery(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        long count = countColdPageQueries(session, () ->
                session.groups().getTopLevelGroupsStream(realm, 0, PAGE_SIZE).collect(Collectors.toList()));
        // The page of groups is selected as entities in one query; hydrating the models must not
        // issue one additional lookup per group.
        assertThat(count, is(1L));
    }

    @TestOnServer
    public void searchByNamePageIsHydratedByASingleQuery(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        long count = countColdPageQueries(session, () ->
                session.groups().searchForGroupByNameStream(realm, "grp", false, GROUP_COUNT - PAGE_SIZE, PAGE_SIZE)
                        .collect(Collectors.toList()));
        assertThat(count, is(1L));
    }

    @TestOnServer
    public void subGroupsPageIsHydratedByASingleQuery(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        GroupModel parent = session.groups().searchForGroupByNameStream(realm, "sub-parent", true, 0, 1)
                .findFirst().orElseGet(() -> session.groups().createGroup(realm, "sub-parent"));
        Set<String> existing = parent.getSubGroupsStream().map(GroupModel::getName).collect(Collectors.toSet());
        for (int i = 0; i < GROUP_COUNT; i++) {
            String name = String.format("sub-%02d", i);
            if (!existing.contains(name)) {
                session.groups().createGroup(realm, name, parent);
            }
        }
        long count = countColdPageQueries(session, () ->
                parent.getSubGroupsStream(null, false, 0, PAGE_SIZE).collect(Collectors.toList()));
        assertThat(count, is(1L));
    }

    /**
     * Measures the statements needed to load a page of groups when the group cache cannot serve them,
     * as after an eviction or on another cluster node. The warmup run collects the ids of the page,
     * registering an invalidation for each id forces the by-id resolution to the store, and clearing
     * the persistence context makes sure the warmup run cannot satisfy those lookups either.
     * ({@code CacheRealmProvider.clear()} is a cluster notification that does not take effect within
     * the running session, so the miss path is forced per group instead.)
     */
    private static long countColdPageQueries(KeycloakSession session, Supplier<List<GroupModel>> loadPage) {
        List<GroupModel> warmup = loadPage.get();
        assertThat(warmup.size(), is(PAGE_SIZE));

        CacheRealmProvider cache = session.getProvider(CacheRealmProvider.class);
        warmup.forEach(group -> cache.registerGroupInvalidation(group.getId()));
        session.getProvider(JpaConnectionProvider.class).getEntityManager().clear();

        return countQueries(session, () -> assertThat(loadPage.get().size(), is(PAGE_SIZE)));
    }

    private static long countQueries(KeycloakSession session, Runnable walk) {
        // Counted through a listener on this Hibernate session rather than the SessionFactory-wide
        // Statistics, so statements issued concurrently by other threads sharing the factory cannot
        // skew the result.
        AtomicLong prepared = new AtomicLong();
        session.getProvider(JpaConnectionProvider.class)
                .getEntityManager()
                .unwrap(Session.class)
                .addEventListeners(new SessionEventListener() {
                    @Override
                    public void jdbcPrepareStatementStart() {
                        prepared.incrementAndGet();
                    }
                });
        walk.run();
        return prepared.get();
    }

    private static class GroupListingRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder r) {
            r.name("group-listing-query-count-realm");
            for (int i = 0; i < GROUP_COUNT; i++) {
                r.groups(String.format("grp-%02d", i));
            }
            return r;
        }
    }
}
