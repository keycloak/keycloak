package org.keycloak.federation.ldap.idm.query;

/**
 * <p>This class can be used to define a query parameter for properties annotated with
 * {@link org.keycloak.federation.ldap.idm.model.AttributeProperty}.
 * </p>
 *
 * @author pedroigor
 */
public class AttributeParameter implements QueryParameter {

    private final String name;

    public AttributeParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
