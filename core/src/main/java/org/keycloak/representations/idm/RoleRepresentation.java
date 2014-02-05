package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleRepresentation {
    protected String id;
    protected String name;
    protected String description;
    protected boolean composite;
    protected Composites composites;

    public static class Composites {
        protected Set<String> realm;
        protected Map<String, List<String>> application;


        public Set<String> getRealm() {
            return realm;
        }

        public void setRealm(Set<String> realm) {
            this.realm = realm;
        }

        public Map<String, List<String>> getApplication() {
            return application;
        }

        public void setApplication(Map<String, List<String>> application) {
            this.application = application;
        }
    }

    public RoleRepresentation() {
    }

    public RoleRepresentation(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Composites getComposites() {
        return composites;
    }

    public void setComposites(Composites composites) {
        this.composites = composites;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isComposite() {
        return composite;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
    }
}
