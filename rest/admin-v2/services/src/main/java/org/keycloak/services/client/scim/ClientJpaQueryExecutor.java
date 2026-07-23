package org.keycloak.services.client.scim;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.admin.api.SortOption;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.model.filter.ScimJPAPredicateEvaluator;
import org.keycloak.services.client.ClientField;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public final class ClientJpaQueryExecutor {

    private static final ClientJpaQueryProvider QUERY_PROVIDER = new ClientJpaQueryProvider();

    private ClientJpaQueryExecutor() {
    }

    public static Stream<ClientModel> findClients(KeycloakSession session, RealmModel realm,
                                                  ScimFilterParser.FilterContext filterContext,
                                                  List<SortOption<ClientField>> sortOptions,
                                                  int offset,
                                                  int limit) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ClientEntity> query = cb.createQuery(ClientEntity.class);
        Root<ClientEntity> root = query.from(ClientEntity.class);
        query.select(root);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("realmId"), realm.getId()));
        predicates.add(root.get("protocol").isNotNull());
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(
                session, AdminPermissionsSchema.CLIENTS, realm, cb, query, root));

        ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(
                QUERY_PROVIDER, ClientJpaQuerySchema.SCHEMAS, cb, root);
        if (filterContext != null) {
            predicates.add(evaluator.visit(filterContext).predicate());
        }

        var q = query.where(predicates.toArray(Predicate[]::new));
        var orders = new ArrayList<>(sortOptions.stream().map(sortOption -> {
            var field = ClientJpaQuerySchema.INSTANCE.getAttributeByPath(sortOption.field().toQueryValue())
                    .getModelAttributeName();
            return sortOption.isAscending() ? cb.asc(root.get(field)) : cb.desc(root.get(field));
        }).toList());

        // add default sort by clientId if no sort options are provided
        if (sortOptions.stream().noneMatch(sortOption -> "clientId".equals(sortOption.field().toQueryValue()))) {
            orders.add(cb.asc(root.get("clientId")));
        }

        q.orderBy(orders);

        return closing(paginateQuery(em.createQuery(q), offset, limit).getResultStream()
                // Resolve through the provider to preserve adapter augmentation behavior.
                .map(clientEntity -> session.clients().getClientById(realm, clientEntity.getId()))
                .filter(Objects::nonNull));
    }
}
