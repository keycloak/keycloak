package org.keycloak.tests.admin.realm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RoleBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * The role listings resolve the composite flag for the whole page in one query rather than per role,
 * so assert the flag each representation carries is still the per-role truth.
 */
@KeycloakIntegrationTest
public class RealmRolesCompositeFlagTest {

    private static final int PAGE_SIZE = 2;

    // More roles than RoleContainerResource resolves the composite flag for in one chunk. Every other
    // role is composite, so each chunk carries flags of its own whichever roles it happens to hold -
    // the listing order is not specified.
    private static final int CHUNKED_ROLE_COUNT = 1100;

    @InjectRealm(config = CompositeFlagRealmConf.class)
    ManagedRealm managedRealm;

    @InjectRealm(ref = "chunked", config = ChunkedRealmConf.class)
    ManagedRealm chunkedRealm;

    @Test
    public void realmRoleListCarriesTheCompositeFlag() {
        assertFlags(managedRealm.admin().roles().list(true));
        assertFlags(managedRealm.admin().roles().list(false));
    }

    @Test
    public void realmRoleSearchCarriesTheCompositeFlag() {
        assertFlags(managedRealm.admin().roles().list("flag-", true));
        assertFlags(managedRealm.admin().roles().list("flag-", false));
    }

    @Test
    public void pagedRealmRoleListCarriesTheCompositeFlag() {
        // The paged branch bypasses the role cache, so the flag is resolved from the database every
        // time rather than from a stored entry.
        assertFlags(allPages(first -> managedRealm.admin().roles().list(first, PAGE_SIZE, true)));
        assertFlags(allPages(first -> managedRealm.admin().roles().list(first, PAGE_SIZE, false)));
    }

    @Test
    public void pagedRealmRoleSearchCarriesTheCompositeFlag() {
        assertFlags(allPages(first -> managedRealm.admin().roles().list("flag-", first, PAGE_SIZE, true)));
        assertFlags(allPages(first -> managedRealm.admin().roles().list("flag-", first, PAGE_SIZE, false)));
    }

    @Test
    public void clientRoleListCarriesTheCompositeFlag() {
        assertFlags(managedRealm.admin().clients().get("flag-client").roles().list(true));
        assertFlags(managedRealm.admin().clients().get("flag-client").roles().list(false));
    }

    @Test
    public void listingLongerThanOneChunkCarriesTheCompositeFlag() {
        Map<String, Boolean> byName = chunkedRealm.admin().roles().list(true).stream()
                .filter(role -> role.getName().startsWith("chunk-"))
                .collect(Collectors.toMap(RoleRepresentation::getName, RoleRepresentation::isComposite));

        assertThat(byName.size(), is(CHUNKED_ROLE_COUNT));
        for (int i = 0; i < CHUNKED_ROLE_COUNT; i++) {
            assertThat("chunk-" + i, byName.get("chunk-" + i), is(isChunkedComposite(i)));
        }
    }

    private static boolean isChunkedComposite(int index) {
        return index % 2 == 0;
    }

    private static List<RoleRepresentation> allPages(IntFunction<List<RoleRepresentation>> page) {
        List<RoleRepresentation> all = new ArrayList<>();
        for (int first = 0; ; first += PAGE_SIZE) {
            List<RoleRepresentation> roles = page.apply(first);
            if (roles.isEmpty()) {
                return all;
            }
            all.addAll(roles);
        }
    }

    private static void assertFlags(List<RoleRepresentation> roles) {
        Map<String, Boolean> byName = roles.stream()
                .filter(role -> role.getName().startsWith("flag-"))
                .collect(Collectors.toMap(RoleRepresentation::getName, RoleRepresentation::isComposite));

        assertThat(byName.get("flag-composite"), is(true));
        assertThat(byName.get("flag-plain"), is(false));
        assertThat(byName.get("flag-child"), is(false));
    }

    private static class CompositeFlagRealmConf implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder builder) {
            builder.realmRoles("flag-child", "flag-plain");
            builder.realmRoles(RoleBuilder.create("flag-composite").realmComposite("flag-child"));

            builder.clients(ClientBuilder.create("flag-client").id("flag-client"));
            builder.clientRoles("flag-client", RoleBuilder.create("flag-child"), RoleBuilder.create("flag-plain"));
            builder.clientRoles("flag-client",
                    RoleBuilder.create("flag-composite").clientComposite("flag-client", "flag-child"));

            return builder;
        }
    }

    private static class ChunkedRealmConf implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder builder) {
            builder.realmRoles("shared-chunk-child");
            for (int i = 0; i < CHUNKED_ROLE_COUNT; i++) {
                builder.realmRoles(isChunkedComposite(i)
                        ? RoleBuilder.create().name("chunk-" + i).realmComposite("shared-chunk-child")
                        : RoleBuilder.create().name("chunk-" + i));
            }
            return builder;
        }
    }
}
