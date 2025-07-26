package org.keycloak.representations.openid_federation;

import java.util.List;

public class NamingConstraints {

    private List<String> permitted;
    private List<String> excluded;

    public List<String> getPermitted() {
        return permitted;
    }

    public void setPermitted(List<String> permitted) {
        this.permitted = permitted;
    }

    public List<String> getExcluded() {
        return excluded;
    }

    public void setExcluded(List<String> excluded) {
        this.excluded = excluded;
    }
}
