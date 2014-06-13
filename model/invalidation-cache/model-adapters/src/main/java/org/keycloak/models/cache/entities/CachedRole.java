package org.keycloak.models.cache.entities;

import org.keycloak.models.RoleModel;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedRole {
    final protected String id;
    final protected String name;
    final protected String description;
    final protected boolean composite;
    final protected Set<String> composites = new HashSet<String>();

    public CachedRole(RoleModel model) {
        composite = model.isComposite();
        description = model.getDescription();
        id = model.getId();
        name = model.getName();
        if (composite) {
            for (RoleModel child : model.getComposites()) {
                composites.add(child.getId());
            }
        }

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isComposite() {
        return composite;
    }

    public Set<String> getComposites() {
        return composites;
    }
}
