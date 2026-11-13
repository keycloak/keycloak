package org.keycloak.tests.model;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.JpaRealmProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
public class RoleCompositeFlagQueryCountTest {

    private static final String REALM = "role-composite-flag-realm";
    private static final String OTHER_REALM = "role-composite-flag-other-realm";

    // COMPOSITE_COUNT parents each holding one leaf, plus PLAIN_COUNT roles with no children.
    private static final int COMPOSITE_COUNT = 5;
    private static final int PLAIN_COUNT = 15;

    @InjectRealm(config = RoleCompositeFlagRealmConfig.class)
    ManagedRealm realm;

    @InjectRealm(ref = "other", config = OtherRealmConfig.class)
    ManagedRealm otherRealm;

    @TestOnServer
    public void flagsWholeBatchInOneQuery(KeycloakSession session) {
        RealmModel realm = realmByName(session, REALM);
        Map<String, String> idsByName = idsByName(session, realm);

        Set<String> requested = idsByName.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("leaf-"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        Set<String> compositeIds = countQueries(session, 1L,
                () -> session.roles().getCompositeRoleIds(realm, requested));

        assertThat(compositeIds, is(expectedCompositeIds(idsByName)));
    }

    @TestOnServer
    public void flagsAParentWhicheverOfItsChildrenIsLookedAt(KeycloakSession session) {
        RealmModel realm = realmByName(session, REALM);
        String parentId = roleIdByName(session, realm, "many-parent");

        // The projection is "select distinct", so a parent with several children must still come back
        // exactly once rather than once per child.
        assertThat(session.roles().getCompositeRoleIds(realm, Set.of(parentId)), is(Set.of(parentId)));
    }

    @TestOnServer
    public void flagsAParentWhoseOnlyChildIsAClientRole(KeycloakSession session) {
        RealmModel realm = realmByName(session, REALM);
        String parentId = roleIdByName(session, realm, "cross-parent");

        assertThat(session.roles().getCompositeRoleIds(realm, Set.of(parentId)), is(Set.of(parentId)));
    }

    @TestOnServer
    public void ignoresRoleIdsFromAnotherRealm(KeycloakSession session) {
        String foreignId = roleIdByName(session, realmByName(session, OTHER_REALM), "other-parent");

        assertThat(session.roles().getCompositeRoleIds(realmByName(session, REALM), Set.of(foreignId)),
                is(Set.of()));
    }

    @TestOnServer
    public void staysExactWhenTheBatchSpansChunks(KeycloakSession session) {
        RealmModel realm = realmByName(session, REALM);
        Map<String, String> idsByName = idsByName(session, realm);
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        // A threshold of 2 splits the batch over many IN queries; the union must still be exact.
        JpaRealmProvider provider = new JpaRealmProvider(session, em, null, null, 2);

        assertThat(provider.getCompositeRoleIds(realm, Set.copyOf(idsByName.values())),
                is(expectedCompositeIds(idsByName)));
    }

    private static RealmModel realmByName(KeycloakSession session, String name) {
        return session.realms().getRealmByName(name);
    }

    private static String roleIdByName(KeycloakSession session, RealmModel realm, String name) {
        return session.roles().getRealmRole(realm, name).getId();
    }

    private static Set<String> expectedCompositeIds(Map<String, String> idsByName) {
        return idsByName.entrySet().stream()
                .filter(e -> e.getKey().startsWith("composite-"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    private static Map<String, String> idsByName(KeycloakSession session, RealmModel realm) {
        return session.roles().getRealmRolesStream(realm)
                .filter(role -> role.getName().startsWith("composite-")
                        || role.getName().startsWith("leaf-")
                        || role.getName().startsWith("plain-"))
                .collect(Collectors.toMap(RoleModel::getName, RoleModel::getId));
    }

    private static <T> T countQueries(KeycloakSession session, long expected, Supplier<T> call) {
        Statistics stats = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        boolean wasEnabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        long count;
        T result;
        try {
            result = call.get();
        } finally {
            count = stats.getPrepareStatementCount();
            stats.setStatisticsEnabled(wasEnabled);
        }
        assertThat(count, is(expected));
        return result;
    }

    private static class RoleCompositeFlagRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder r) {
            r.name(REALM);

            for (int i = 0; i < COMPOSITE_COUNT; i++) {
                String leafName = "leaf-" + i;
                r.realmRoles(leafName);
                r.realmRoles(RoleBuilder.create().name("composite-" + i).realmComposite(leafName));
            }
            for (int i = 0; i < PLAIN_COUNT; i++) {
                r.realmRoles("plain-" + i);
            }

            r.realmRoles("many-child-a", "many-child-b", "many-child-c");
            r.realmRoles(RoleBuilder.create().name("many-parent")
                    .realmComposite("many-child-a").realmComposite("many-child-b").realmComposite("many-child-c"));

            r.clients(ClientBuilder.create("cross-client").id("cross-client"));
            r.clientRoles("cross-client", RoleBuilder.create("cross-child"));
            r.realmRoles(RoleBuilder.create().name("cross-parent").clientComposite("cross-client", "cross-child"));

            return r;
        }
    }

    private static class OtherRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder r) {
            r.name(OTHER_REALM);
            r.realmRoles("other-child");
            r.realmRoles(RoleBuilder.create().name("other-parent").realmComposite("other-child"));
            return r;
        }
    }
}
