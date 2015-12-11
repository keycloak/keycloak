package org.keycloak.models.cache.entities;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.RealmCache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedClientTemplate implements Serializable {

    private String id;
    private String name;
    private String description;
    private String realm;
    private String protocol;
    private Set<ProtocolMapperModel> protocolMappers = new HashSet<ProtocolMapperModel>();

    public CachedClientTemplate(RealmCache cache, RealmProvider delegate, RealmModel realm, ClientTemplateModel model) {
        id = model.getId();
        name = model.getName();
        description = model.getDescription();
        this.realm = realm.getId();
        protocol = model.getProtocol();
        for (ProtocolMapperModel mapper : model.getProtocolMappers()) {
            this.protocolMappers.add(mapper);
        }
    }
    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getRealm() {
        return realm;
    }
    public Set<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers;
    }

    public String getProtocol() {
        return protocol;
    }
}
