package org.keycloak.models.cache.infinispan.organization;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.OrganizationInvitationModel;
import org.keycloak.models.OrganizationInvitationModel.Filter;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.InvitationManager;

record InfinispanInvitationManager(InvitationManager delegate) implements InvitationManager {

    @Override
    public OrganizationInvitationModel create(OrganizationModel organization, String email, String firstName, String lastName) {
        return delegate().create(organization, email, firstName, lastName);
    }

    @Override
    public OrganizationInvitationModel getById(String id) {
        return delegate().getById(id);
    }

    @Override
    public Stream<OrganizationInvitationModel> getAllStream(OrganizationModel organization, Map<Filter, String> attributes, Integer first, Integer max) {
        return delegate().getAllStream(organization, attributes, first, max);
    }

    @Override
    public boolean remove(String id) {
        return delegate().remove(id);
    }
}
