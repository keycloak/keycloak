package org.keycloak.models.cache.entities;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:jli@vizuri.com">Jiehuan Li</a>
 * @version $Revision: 1 $
 */
public class CachedRole {
    final protected String id;
    final protected String name;
    final protected String realm;
    final protected String description;
    final protected boolean composite;
    final protected Set<String> composites = new HashSet<String>();
    final protected String federationLink;
    final protected Map<String, String> attributes = new HashMap<String, String>();

    public CachedRole(RoleModel model, RealmModel realm) {
        composite = model.isComposite();
        description = model.getDescription();
        id = model.getId();
        name = model.getName();
        this.realm = realm.getId();
        if (composite) {
            for (RoleModel child : model.getComposites()) {
                composites.add(child.getId());
            }
        }
        federationLink = model.getFederationLink();
        Map<String, String> attrMap = model.getAttributes();
        for (String key: attrMap.keySet()) {
        	attributes.put(key, attrMap.get(key));
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRealm() {
        return realm;
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
    
    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    public String getFederationLink() {
        return federationLink;
    }
}
