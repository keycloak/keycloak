package org.keycloak.testsuite.model.role;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RequireProvider(RealmProvider.class)
@RequireProvider(RoleProvider.class)
@RequireProvider(JpaConnectionProvider.class)
public class CompositeRoleExpansionQueryCountTest extends KeycloakModelTest {

    // Deep composite chain r0 -> r1 -> ... -> r9 (r9 is a leaf): V = 10 nodes, C = 9 composite nodes.
    private static final int CHAIN_LENGTH = 10;

    // Wide tree: root -> BRANCHING mids -> BRANCHING leaves each. V = 1 + 5 + 25 = 31, depth = 3 levels.
    private static final int BRANCHING = 5;
    private static final int BUSHY_SIZE = 1 + BRANCHING + BRANCHING * BRANCHING;
    private static final int BUSHY_LEVELS = 3;

    private String realmId;
    private String rootRoleId;
    private String bushyRootId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "composite-expansion-realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realmId = realm.getId();

        RoleModel previous = null;
        for (int i = 0; i < CHAIN_LENGTH; i++) {
            RoleModel role = s.roles().addRealmRole(realm, "chain-" + i);
            if (previous == null) {
                rootRoleId = role.getId();
            } else {
                previous.addCompositeRole(role);
            }
            previous = role;
        }

        RoleModel bushyRoot = s.roles().addRealmRole(realm, "bushy-root");
        bushyRootId = bushyRoot.getId();
        for (int i = 0; i < BRANCHING; i++) {
            RoleModel mid = s.roles().addRealmRole(realm, "bushy-mid-" + i);
            bushyRoot.addCompositeRole(mid);
            for (int j = 0; j < BRANCHING; j++) {
                mid.addCompositeRole(s.roles().addRealmRole(realm, "bushy-leaf-" + i + "-" + j));
            }
        }
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void droppingIsCompositePreQueryHalvesGetChildRolesOnCompositeNodes() {
        long currentCount = withRealm(realmId, (s, realm) -> {
            RoleModel root = s.roles().getRoleById(realm, rootRoleId);
            return countQueries(s, () -> RoleUtils.expandCompositeRoles(Set.of(root)));
        });

        long prePatchCount = withRealm(realmId, (s, realm) -> {
            RoleModel root = s.roles().getRoleById(realm, rootRoleId);
            return countQueries(s, () -> expandWithIsCompositePreQuery(Set.of(root)));
        });

        // Current code: one getChildRoles per visited node (getCompositesStream on every node).
        assertThat(currentCount, is((long) CHAIN_LENGTH));                  // V = 10
        // Pre-patch code: isComposite() on every node + getCompositesStream() on composite nodes.
        assertThat(prePatchCount, is((long) (2 * CHAIN_LENGTH - 1)));       // V + C = 19
        // Saved exactly C = number of composite nodes.
        assertThat(prePatchCount - currentCount, is((long) (CHAIN_LENGTH - 1))); // 9
    }

    @Test
    public void levelWiseBatchingCollapsesGetChildRolesToOnePerLevel() {
        Set<String> perNodeIds = new HashSet<>();
        long perNodeCount = withRealm(realmId, (s, realm) -> {
            RoleModel root = s.roles().getRoleById(realm, bushyRootId);
            return countQueries(s, () -> collectIds(RoleUtils.expandCompositeRoles(Set.of(root)), perNodeIds));
        });

        Set<String> batchedIds = new HashSet<>();
        long batchedCount = withRealm(realmId, (s, realm) -> {
            RoleModel root = s.roles().getRoleById(realm, bushyRootId);
            return countQueries(s, () -> collectIds(RoleUtils.expandCompositeRoles(s, realm, Set.of(root)), batchedIds));
        });

        // Both expansions yield the same role set ...
        assertThat(perNodeIds.size(), is(BUSHY_SIZE));    // 31
        assertThat(batchedIds, is(perNodeIds));
        // ... but the per-node walk issues one getChildRoles query per node ...
        assertThat(perNodeCount, is((long) BUSHY_SIZE));  // 31
        // ... whereas level-wise batching issues a single query per breadth-first level.
        assertThat(batchedCount, is((long) BUSHY_LEVELS)); // 3
    }

    private static void collectIds(Set<RoleModel> roles, Set<String> into) {
        roles.forEach(role -> into.add(role.getId()));
    }

    private long countQueries(KeycloakSession session, Runnable walk) {
        Statistics stats = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        walk.run();
        // The only JDBC statements during the walk are the getChildRoles named-query executions:
        // getCompositesStream() hydrates the child RoleEntity rows, so the subsequent getRoleById
        // em.find calls are served from the persistence context without extra statements.
        return stats.getPrepareStatementCount();
    }

    // Pre-patch walk: keeps the isComposite() guard so both variants run against the same database.
    private static Set<RoleModel> expandWithIsCompositePreQuery(Set<RoleModel> roles) {
        Set<RoleModel> visited = new HashSet<>();
        Set<RoleModel> result = new HashSet<>();
        for (RoleModel role : roles) {
            if (visited.contains(role)) {
                continue;
            }
            Deque<RoleModel> stack = new ArrayDeque<>();
            stack.add(role);
            while (!stack.isEmpty()) {
                RoleModel current = stack.pop();
                result.add(current);
                if (current.isComposite()) {
                    current.getCompositesStream()
                            .filter(r -> !visited.contains(r))
                            .forEach(r -> {
                                visited.add(r);
                                stack.add(r);
                            });
                }
            }
        }
        return result;
    }
}
