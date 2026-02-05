package org.keycloak.scim.resource.group;

import java.util.List;
import java.util.Set;

import org.keycloak.scim.resource.ScimResource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Group extends ScimResource {

    public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("members")
    private List<Member> members;

    @Override
    public Type getType() {
        return Type.Group;
    }

    @Override
    public String getPath() {
        return "Groups";
    }

    @Override
    public Set<String> getSchemas() {
        return Set.of(SCHEMA);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }
}
