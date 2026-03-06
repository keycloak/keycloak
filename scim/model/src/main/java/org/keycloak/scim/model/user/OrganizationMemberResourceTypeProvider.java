package org.keycloak.scim.model.user;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.scim.filter.ScimFilterParser.FilterContext;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.user.User;

public class OrganizationMemberResourceTypeProvider extends UserResourceTypeProvider {

    private final OrganizationModel organization;

    public OrganizationMemberResourceTypeProvider(KeycloakSession session, OrganizationModel organization) {
        super(session);
        this.organization = organization;
    }

    @Override
    public User create(User resource) {
        User user = super.create(resource);

        if (user != null) {
            OrganizationProvider provider = getOrganizationProvider();
            provider.addMember(organization, getModel(user.getId()));
        }

        return user;
    }

    @Override
    public User update(User resource) {
        checkMembership(resource.getId());
        return super.update(resource);
    }

    @Override
    public void patch(User existing, List<PatchOperation> operations) {
        checkMembership(existing.getId());
        super.patch(existing, operations);
    }

    @Override
    public boolean delete(String id) {
        checkMembership(id);
        return super.delete(id);
    }

    @Override
    public User get(String id) {
        if (!organization.isMember(getModel(id))) {
            return null;
        }

        return super.get(id);
    }

    @Override
    protected From<?, ?> doGetRootPath(CriteriaBuilder builder, CriteriaQuery<?> query) {
        Root<UserGroupMembershipEntity> groupMembership = query.from(UserGroupMembershipEntity.class);
        From userJoin = groupMembership.join("user");

        query.select(userJoin);

        return userJoin;
    }

    @Override
    protected List<Predicate> getUserPredicates(FilterContext filterContext, CriteriaBuilder cb, CriteriaQuery<?> query, From<?, ?> root) {
        var predicates = super.getUserPredicates(filterContext, cb, query, root);
        var group = getOrganizationProvider().getOrganizationGroup(organization);

        query.getRoots().stream().filter(r -> r.getJavaType().equals(UserGroupMembershipEntity.class))
                .findAny()
                .ifPresent(value -> predicates.add(cb.equal(value.get("groupId"), group.getId())));

        return predicates;
    }

    @Override
    protected Stream<UserModel> doGetAll(RealmModel realm, Integer firstResult, Integer maxResults) {
        return getOrganizationProvider().getMembersStream(organization, Map.of(), true, firstResult, maxResults);
    }

    @Override
    protected long doCountAll(RealmModel realm) {
        return getOrganizationProvider().getMembersCount(organization);
    }

    private void checkMembership(String id) {
        UserModel model = getModel(id);

        if (!organization.isMember(model)) {
            throw new ModelValidationException("User is not a member of the organization " + organization.getAlias());
        }
    }

    private OrganizationProvider getOrganizationProvider() {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        return provider;
    }
}
