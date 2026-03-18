package org.keycloak.organization.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationInvitationModel;
import org.keycloak.models.OrganizationInvitationModel.Filter;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.jpa.entities.OrganizationInvitationEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.InvitationManager;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation.Status;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

record JpaInvitationManager(KeycloakSession session, EntityManager em) implements InvitationManager {

    @Override
    public OrganizationInvitationModel create(OrganizationModel organization, String email,
                                              String firstName, String lastName) {
        String id = KeycloakModelUtils.generateId();
        OrganizationInvitationEntity entity = new OrganizationInvitationEntity(
                id, organization.getId(), email.trim(),
                firstName != null ? firstName.trim() : null,
                lastName != null ? lastName.trim() : null
        );

        entity.setExpiresAt(getExpiration());

        em.persist(entity);

        return entity;
    }

    @Override
    public OrganizationInvitationModel getById(String id) {
        return em.find(OrganizationInvitationEntity.class, id);
    }

    @Override
    public Stream<OrganizationInvitationModel> getAllStream(OrganizationModel organization, Map<Filter, String> attributes, Integer first, Integer max) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<OrganizationInvitationEntity> query = builder.createQuery(OrganizationInvitationEntity.class);
        Root<OrganizationInvitationEntity> root = query.from(OrganizationInvitationEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("organizationId"), organization.getId()));

        for (Entry<Filter, String> filter : attributes.entrySet()) {
            switch (filter.getKey()) {
                case EMAIL -> {
                    String escapedValue = filter.getValue().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("*", "%");
                    predicates.add(builder.like(root.get("email"), "%" + escapedValue + "%", '\\'));
                }
                case STATUS -> {
                    Status value = Status.valueOf(filter.getValue());

                    if (Status.EXPIRED.equals(value)) {
                        predicates.add(builder.lessThan(root.get("expiresAt"), Time.currentTime()));
                    } else {
                        predicates.add(builder.greaterThanOrEqualTo(root.get("expiresAt"), Time.currentTime()));
                    }
                }
                case FIRST_NAME -> {
                    String escapedValue = filter.getValue().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("*", "%");
                    predicates.add(builder.like(builder.lower(root.get("firstName")), "%" + escapedValue + "%", '\\'));
                }
                case LAST_NAME -> {
                    String escapedValue = filter.getValue().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("*", "%");
                    predicates.add(builder.like(builder.lower(root.get("lastName")), "%" + escapedValue + "%", '\\'));
                }
                case SEARCH -> {
                    String escapedValue = filter.getValue().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("*", "%");
                    String searchValue = "%" + escapedValue + "%";

                    Predicate emailPredicate = builder.like(builder.lower(root.get("email")), searchValue, '\\');
                    Predicate firstNamePredicate = builder.like(builder.lower(root.get("firstName")), searchValue, '\\');
                    Predicate lastNamePredicate = builder.like(builder.lower(root.get("lastName")), searchValue, '\\');

                    predicates.add(builder.or(emailPredicate, firstNamePredicate, lastNamePredicate));
                }
            }
        }

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(builder.asc(root.get("id")));

        TypedQuery<OrganizationInvitationEntity> typedQuery = em.createQuery(query);

        return closing(paginateQuery(typedQuery, first, max).getResultStream().map(OrganizationInvitationModel.class::cast));
    }

    @Override
    public boolean remove(String id) {
        OrganizationInvitationEntity invitation = (OrganizationInvitationEntity) getById(id);

        if (invitation == null) {
            return false;
        }

        em.remove(invitation);

        return true;
    }

    private int getExpiration() {
        return Time.currentTime() + session.getContext().getRealm().getActionTokenGeneratedByAdminLifespan();
    }
}
