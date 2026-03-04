package org.keycloak.testframework.realm;


import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;

public class OrganizationConfigBuilder {

    private final OrganizationRepresentation rep;

    private OrganizationConfigBuilder(OrganizationRepresentation rep) {
        this.rep = rep;
    }

    public static OrganizationConfigBuilder create() {
        OrganizationRepresentation rep = new OrganizationRepresentation();
        rep.setEnabled(true);
        return new OrganizationConfigBuilder(rep);
    }

    public static OrganizationConfigBuilder update(OrganizationRepresentation rep) {
        return new OrganizationConfigBuilder(rep);
    }

    public OrganizationConfigBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public OrganizationConfigBuilder alias(String alias) {
        rep.setAlias(alias);
        return this;
    }

    public OrganizationConfigBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public OrganizationConfigBuilder domain(String domain) {
        rep.addDomain(new OrganizationDomainRepresentation(domain));
        return this;
    }

    public OrganizationRepresentation build() {
        return rep;
    }
}
