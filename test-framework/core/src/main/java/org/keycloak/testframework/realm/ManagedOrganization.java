package org.keycloak.testframework.realm;


import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.injection.ManagedTestResource;

public class ManagedOrganization extends ManagedTestResource {

    private final OrganizationRepresentation representation;
    private final OrganizationResource resource;

    public ManagedOrganization(OrganizationRepresentation representation, OrganizationResource resource) {
        this.representation = representation;
        this.resource = resource;
    }

    @Override
    public void runCleanup() {
        // not yet implemented
    }

    public OrganizationResource admin() {
        return resource;
    }
}
