package org.keycloak.testframework.realm;

import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectOrganization;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.util.ApiUtil;

public class OrganizationSupplier implements Supplier<ManagedOrganization, InjectOrganization> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<ManagedOrganization, InjectOrganization> instanceContext) {
        return DependenciesBuilder.create(ManagedRealm.class, instanceContext.getAnnotation().realmRef()).build();
    }

    @Override
    public ManagedOrganization getValue(InstanceContext<ManagedOrganization, InjectOrganization> instanceContext) {
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, instanceContext.getAnnotation().realmRef());
        RealmRepresentation realmRep = realm.admin().toRepresentation();

        if (!realmRep.isOrganizationsEnabled()) {
            realm.updateWithCleanup(r -> r.organizationsEnabled(true));
        }

        String attachTo = instanceContext.getAnnotation().attachTo();
        boolean managed = attachTo.isEmpty();

        OrganizationRepresentation representation;

        if (managed) {
            OrganizationConfig config = SupplierHelpers.getInstanceWithInjectedFields(instanceContext.getAnnotation().config(), instanceContext);
            representation = config.configure(OrganizationConfigBuilder.create()).build();

            if (representation.getAlias() == null) {
                representation.setAlias(SupplierHelpers.createName(instanceContext));
            }

            if (representation.getName() == null) {
                representation.setName(representation.getAlias());
            }

            Response response = realm.admin().organizations().create(representation);
            if (Status.CONFLICT.equals(Status.fromStatusCode(response.getStatus()))) {
                throw new IllegalStateException("Organization already exist with alias: " + representation.getAlias());
            }
            representation.setId(ApiUtil.getCreatedId(response));
        } else {
            List<OrganizationRepresentation> organizations = realm.admin().organizations().search(attachTo, true, -1, -1);

            if (organizations.isEmpty()) {
                throw new IllegalStateException("No organization found with alias: " + attachTo);
            }

            if (organizations.size() > 1) {
                throw new IllegalStateException("Multiple organizations found with alias: " + attachTo);
            }

            representation = organizations.get(0);
        }

        instanceContext.addNote("managed", managed);

        OrganizationResource resource = realm.admin().organizations().get(representation.getId());
        return new ManagedOrganization(representation, resource);
    }

    @Override
    public boolean compatible(InstanceContext<ManagedOrganization, InjectOrganization> a, RequestedInstance<ManagedOrganization, InjectOrganization> b) {
        InjectOrganization aa = a.getAnnotation();
        InjectOrganization ba = b.getAnnotation();
        return aa.config().equals(ba.config());
    }

    @Override
    public void close(InstanceContext<ManagedOrganization, InjectOrganization> instanceContext) {
        if (instanceContext.getNote("managed", Boolean.class)) {
            try {
                instanceContext.getValue().admin().delete().close();
            } catch (NotFoundException ex) {}
        }
    }

}
