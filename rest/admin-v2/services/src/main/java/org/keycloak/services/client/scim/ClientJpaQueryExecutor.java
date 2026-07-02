package org.keycloak.services.client.scim;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.model.filter.ScimJPAPredicateEvaluator;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public final class ClientJpaQueryExecutor {

    private static final ClientJpaQueryProvider QUERY_PROVIDER = new ClientJpaQueryProvider();

    private ClientJpaQueryExecutor() {
    }

    public static Stream<String> findClientIds(KeycloakSession session, RealmModel realm,
                                               ScimFilterParser.FilterContext filterContext,
                                               int offset, int limit) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<ClientEntity> root = query.from(ClientEntity.class);
        query.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("realmId"), realm.getId()));
        predicates.add(root.get("protocol").isNotNull());
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(
                session, AdminPermissionsSchema.CLIENTS, realm, cb, query, root));

        ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(
                QUERY_PROVIDER, ClientJpaQuerySchema.SCHEMAS, cb, root);
        predicates.add(evaluator.visit(filterContext).predicate());

        query.where(predicates.toArray(Predicate[]::new)).orderBy(cb.asc(root.get("clientId")));

        return closing(paginateQuery(em.createQuery(query), offset, limit).getResultStream());
    }
}
