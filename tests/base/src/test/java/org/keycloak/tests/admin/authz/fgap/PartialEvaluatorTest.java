package org.keycloak.tests.admin.authz.fgap;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationContext;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluator;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.representations.idm.authorization.ResourceType;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = PartialEvaluatorServerConfig.class)
public class PartialEvaluatorTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @Test
    public void deniedResourcesUseBindParametersForOrdinaryCollections() {
        String realmName = realm.getName();
        runOnServer.run((RunOnServer) session -> {
            String sql = executeQuery(session, realmName, Set.of("allowed"), Set.of("denied"));
            assertEquals(2, countParameters(sql));
        });
    }

    @Test
    public void deniedResourcesDoNotUseBindParametersForOversizedCollections() {
        String realmName = realm.getName();
        runOnServer.run((RunOnServer) session -> {
            // threshold is 5, so 10 denied IDs triggers literals
            String sql = executeQuery(session, realmName, Set.of("allowed"), createIds());
            // only the 1 allowed ID should be a bind parameter
            assertEquals(1, countParameters(sql));
        });
    }

    @Test
    public void allowedResourcesUseBindParametersForOrdinaryCollections() {
        String realmName = realm.getName();
        runOnServer.run((RunOnServer) session -> {
            String sql = executeQuery(session, realmName, Set.of("allowed"), Set.of());
            assertEquals(1, countParameters(sql));
        });
    }

    @Test
    public void allowedResourcesDoNotUseBindParametersForOversizedCollections() {
        String realmName = realm.getName();
        runOnServer.run((RunOnServer) session -> {
            // threshold is 5, so 10 allowed IDs triggers literals
            String sql = executeQuery(session, realmName, createIds(), Set.of());
            assertEquals(0, countParameters(sql));
        });
    }

    private static String executeQuery(org.keycloak.models.KeycloakSession keycloakSession, String realmName, Set<String> allowedIds, Set<String> deniedIds) {
        RealmModel realmModel = keycloakSession.realms().getRealmByName(realmName);
        keycloakSession.getContext().setRealm(realmModel);
        EntityManager em = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
        SessionFactory sf = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        AtomicReference<String> lastSql = new AtomicReference<>();

        try (Session hibernateSession = sf.withOptions()
                .statementInspector((UnaryOperator<String>) sql -> {
                    if (sql.startsWith("select")) {
                        lastSql.set(sql);
                    }
                    return sql;
                })
                .openSession()) {

            CriteriaBuilder builder = hibernateSession.getCriteriaBuilder();
            CriteriaQuery<GroupEntity> query = builder.createQuery(GroupEntity.class);
            Root<GroupEntity> root = query.from(GroupEntity.class);
            ResourceType resourceType = AdminPermissionsSchema.GROUPS;
            PartialEvaluationContext context = new PartialEvaluationContext(
                    keycloakSession, resourceType, allowedIds, deniedIds, null, builder, query, root);
            List<Predicate> predicates = new PartialEvaluator().buildPredicates(context);

            query.select(root).where(predicates.toArray(Predicate[]::new));
            hibernateSession.createQuery(query).getResultList();

            return lastSql.get();
        }
    }

    private static Set<String> createIds() {
        Set<String> ids = new LinkedHashSet<>();
        ids.add("group-'quoted");
        for (int i = 1; i < 10; i++) {
            ids.add("group-" + i);
        }
        return ids;
    }

    private static int countParameters(String sql) {
        return (int) sql.chars().filter(c -> c == '?').count();
    }
}
