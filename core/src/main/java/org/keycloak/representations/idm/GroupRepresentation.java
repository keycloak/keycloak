package org.keycloak.representations.idm;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupRepresentation {
    protected String id;
    protected String name;
    protected Map<String, List<String>>  attributes;
    protected List<String> realmRoles;
    protected Map<String, List<String>> clientRoles;
    protected List<GroupRepresentation> subGroups;

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

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public Map<String, List<String>> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(Map<String, List<String>> clientRoles) {
        this.clientRoles = clientRoles;
    }


    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>>  attributes) {
        this.attributes = attributes;
    }

    public GroupRepresentation singleAttribute(String name, String value) {
        if (this.attributes == null) attributes = new HashMap<>();
        attributes.put(name, Arrays.asList(value));
        return this;
    }

    public List<GroupRepresentation> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<GroupRepresentation> subGroups) {
        this.subGroups = subGroups;
    }
}
