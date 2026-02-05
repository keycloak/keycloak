package org.keycloak.scim.model.user;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.GroupModel;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.schema.AbstractScimSchema;

public final class GroupCoreSchema extends AbstractScimSchema<GroupModel, Group> {

    private static final Map<String, AttributeMapper<GroupModel, Group>> ATTRIBUTE_MAPPERS = new HashMap<>();

    static {
        ATTRIBUTE_MAPPERS.put("displayName", new AttributeMapper<>(
                new ModelAttributeMapper<>((model, s) -> model.getName(), (model, name, value) -> model.setName(value)),
                new ResourceTypeAttributeMapper<>(Group::setDisplayName)));
    }


    public GroupCoreSchema() {
        super(ATTRIBUTE_MAPPERS);
    }

    @Override
    protected Set<String> getAttributeNames(GroupModel model) {
        return Set.of("name");
    }

    @Override
    protected String getAttributeValue(GroupModel model, String name) {
        if (name.equals("name")) {
            return model.getName();
        }
        return null;
    }

    @Override
    protected String getScimAttributeName(GroupModel model, String name) {
        if (name.equals("name")) {
            return "displayName";
        }
        return null;
    }

    @Override
    protected String getScimSchema(GroupModel model, String name) {
        return "urn:ietf:params:scim:schemas:core:2.0:Group";
    }
}
