package org.keycloak.testframework.realm;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.InjectOrganization;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.util.ApiUtil;

public class OrganizationSupplier implements Supplier<ManagedOrganization, InjectOrganization> {

    private static final String ORG_UUID_KEY = "organizationUuid";

    @Override
    public List<Dependency> getDependencies(RequestedInstance<ManagedOrganization, InjectOrganization> instanceContext) {
        return DependenciesBuilder.create(ManagedRealm.class, instanceContext.getAnnotation().realmRef()).build();
    }

    @Override
    public ManagedOrganization getValue(InstanceContext<ManagedOrganization, InjectOrganization> instanceContext) {
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, instanceContext.getAnnotation().realmRef());

        if (!Boolean.TRUE.equals(realm.admin().toRepresentation().isOrganizationsEnabled())) {
            throw new IllegalStateException("Organizations are not enabled for realm '" + realm.getName()
                    + "'. Configure the realm with a RealmConfig calling RealmBuilder.organizationsEnabled(true).");
        }

        OrganizationConfig config = SupplierHelpers.getInstanceWithInjectedFields(instanceContext.getAnnotation().config(), instanceContext);
        OrganizationRepresentation organizationRepresentation = config.configure(OrganizationBuilder.create()).build();

        if (organizationRepresentation.getName() == null) {
            organizationRepresentation.setName(SupplierHelpers.createName(instanceContext));
        }

        try (Response response = realm.admin().organizations().create(organizationRepresentation)) {
            expectOrganizationCreated(response, organizationRepresentation);
            String uuid = ApiUtil.getCreatedId(response);

            instanceContext.addNote(ORG_UUID_KEY, uuid);

            OrganizationResource organizationResource = realm.admin().organizations().get(uuid);
            organizationRepresentation.setId(uuid);

            return new ManagedOrganization(organizationRepresentation, organizationResource);
        }
    }

    private void expectOrganizationCreated(Response response, OrganizationRepresentation organizationRepresentation) {
        if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
            throw new IllegalStateException("Organization '%s' already exists".formatted(organizationRepresentation.getName()));
        }
        ApiUtil.expectStatus(response, "create organization '%s'".formatted(organizationRepresentation.getName()), Status.CREATED);
    }

    @Override
    public boolean compatible(InstanceContext<ManagedOrganization, InjectOrganization> a, RequestedInstance<ManagedOrganization, InjectOrganization> b) {
        InjectOrganization aa = a.getAnnotation();
        InjectOrganization ba = b.getAnnotation();
        return aa.config().equals(ba.config()) && aa.realmRef().equals(ba.realmRef());
    }

    @Override
    public void close(InstanceContext<ManagedOrganization, InjectOrganization> instanceContext) {
        ManagedOrganization organization = instanceContext.getValue();
        try (Response response = organization.admin().delete()) {
            // The organization may already have been deleted by the test itself
            ApiUtil.expectStatus(response, "delete organization '%s'".formatted(organization.getName()),
                    Status.NO_CONTENT, Status.NOT_FOUND);
        }
    }

}
