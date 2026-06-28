package org.keycloak.tests.model;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
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
public class CompositeRoleExpansionQueryCountTest {

    // Deep composite chain r0 -> r1 -> ... -> r9 (r9 is a leaf): V = 10 nodes, C = 9 composite nodes.
    private static final int CHAIN_LENGTH = 10;

    // Wide tree: root -> BRANCHING mids -> BRANCHING leaves each. V = 1 + 5 + 25 = 31, depth = 3 levels.
    private static final int BRANCHING = 5;
    private static final int BUSHY_SIZE = 1 + BRANCHING + BRANCHING * BRANCHING;
    private static final int BUSHY_LEVELS = 3;

    @InjectRealm(config = CompositeRoleExpansionRealmConfig.class)
    ManagedRealm realm;

    @TestOnServer
    public void droppingIsCompositePreQueryHalvesGetChildRolesOnCompositeNodes(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        RoleModel root = session.roles().getRealmRole(realm, "chain-0");
        long currentCount = countQueries(session, () -> RoleUtils.expandCompositeRoles(Set.of(root)));
        // Current batched code: one query per breadth-first level (equals V for a chain since depth = V).
        assertThat(currentCount, is((long) CHAIN_LENGTH));                  // L = V = 10
    }

    @TestOnServer
    public void levelWiseBatchingCollapsesGetChildRolesToOnePerLevel(KeycloakSession session) {
        Set<String> batchedIds = new HashSet<>();
        RealmModel realm = session.getContext().getRealm();
        RoleModel root = session.roles().getRealmRole(realm, "bushy-root");
        long batchedCount = countQueries(session, () -> collectIds(RoleUtils.expandCompositeRoles(Set.of(root)), batchedIds));
        assertThat(batchedIds.size(), is(BUSHY_SIZE)); // 31
        assertThat(batchedCount, is((long) BUSHY_LEVELS)); // 3
    }

    private static void collectIds(Set<RoleModel> roles, Set<String> into) {
        roles.forEach(role -> into.add(role.getId()));
    }

    private static long countQueries(KeycloakSession session, Runnable walk) {
        Statistics stats = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        walk.run();
        return stats.getPrepareStatementCount();
    }

    private static class CompositeRoleExpansionRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder r) {
            r.name("composite-role-expansion-realm");

            RoleBuilder bushyRoot = RoleBuilder.create().name("bushy-root");
            r.realmRoles(bushyRoot);
            for (int i = 0; i < BRANCHING; i++) {
                String name = "bushy-mid-" + i;
                RoleBuilder bushyMid = RoleBuilder.create().name(name);
                r.realmRoles(bushyMid);
                bushyRoot.realmComposite(name);
                for (int j = 0; j < BRANCHING; j++) {
                    String leafName = "bushy-leaf-" + i + "-" + j;
                    r.realmRoles(leafName);
                    bushyMid.realmComposite(leafName);
                }
            }

            RoleBuilder previous = null;
            for (int i = 0; i < CHAIN_LENGTH; i++) {
                String name = "chain-" + i;
                RoleBuilder role = RoleBuilder.create().name(name);
                r.realmRoles(role);
                if (previous != null) {
                    previous.realmComposite(name);
                }
                previous = role;
            }

            return r;
        }
    }
}
