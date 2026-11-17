package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.OrganizationDomainRepresentation;

public class OrganizationDomainBuilder extends Builder<OrganizationDomainRepresentation> {

    private OrganizationDomainBuilder(OrganizationDomainRepresentation rep) {
        super(rep);
    }

    public static OrganizationDomainBuilder create() {
        return new OrganizationDomainBuilder(new OrganizationDomainRepresentation());
    }

    public static OrganizationDomainBuilder create(String name) {
        return create().name(name);
    }

    public static OrganizationDomainBuilder update(OrganizationDomainRepresentation rep) {
        return new OrganizationDomainBuilder(rep);
    }

    public OrganizationDomainBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public OrganizationDomainBuilder verified(boolean verified) {
        rep.setVerified(verified);
        return this;
    }

    public OrganizationDomainBuilder verified() {
        return verified(true);
    }

}
