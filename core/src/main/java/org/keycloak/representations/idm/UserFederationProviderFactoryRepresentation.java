package org.keycloak.representations.idm;

import java.util.Set;

/**
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class UserFederationProviderFactoryRepresentation {

    private String id;
    private Set<String> options;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getOptions() {
        return options;
    }

    public void setOptions(Set<String> options) {
        this.options = options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserFederationProviderFactoryRepresentation that = (UserFederationProviderFactoryRepresentation) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
