package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;

public class OrganizationBuilder extends Builder<OrganizationRepresentation> {

    private OrganizationBuilder(OrganizationRepresentation rep) {
        super(rep);
    }

    public static OrganizationBuilder create() {
        return new OrganizationBuilder(new OrganizationRepresentation()).enabled(true);
    }

    public static OrganizationBuilder create(String name) {
        return create().name(name);
    }

    public static OrganizationBuilder update(OrganizationRepresentation rep) {
        return new OrganizationBuilder(rep);
    }

    public OrganizationBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public OrganizationBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public OrganizationBuilder alias(String alias) {
        rep.setAlias(alias);
        return this;
    }

    public OrganizationBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public OrganizationBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public OrganizationBuilder redirectUrl(String redirectUrl) {
        rep.setRedirectUrl(redirectUrl);
        return this;
    }

    public OrganizationBuilder domains(String... domains) {
        return domains(Arrays.stream(domains).map(OrganizationDomainBuilder::create).toArray(OrganizationDomainBuilder[]::new));
    }

    public OrganizationBuilder domains(OrganizationDomainBuilder... domains) {
        return domains(Arrays.stream(domains).map(OrganizationDomainBuilder::build).toArray(OrganizationDomainRepresentation[]::new));
    }

    public OrganizationBuilder domains(OrganizationDomainRepresentation... domains) {
        for(var domain : domains) {
            rep.addDomain(domain);
        }
        return this;
    }

    public OrganizationBuilder removeDomains(String... domains) {
        for(var domain : domains) {
            rep.removeDomain(new OrganizationDomainRepresentation(domain));
        }
        return this;
    }

    public OrganizationBuilder attribute(String key, String... value) {
        rep.setAttributes(combine(rep.getAttributes(), key, value));
        return this;
    }

    public OrganizationBuilder attributes(Map<String, List<String>> attributes) {
        rep.setAttributes(combine(rep.getAttributes(), attributes));
        return this;
    }

    public OrganizationBuilder removeAttributes(String... keys) {
        rep.setAttributes(removeKeys(rep.getAttributes(), keys));
        return this;
    }

}
