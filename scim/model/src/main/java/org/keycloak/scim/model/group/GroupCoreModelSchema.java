package org.keycloak.scim.model.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.keycloak.models.GroupModel;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.schema.AbstractModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.schema.attribute.AttributeMapper;

public final class GroupCoreModelSchema extends AbstractModelSchema<GroupModel, Group> {

    private static final List<Attribute<GroupModel, Group>> ATTRIBUTE_MAPPERS = new ArrayList<>();

    static {
        ATTRIBUTE_MAPPERS.add(new Attribute<>("displayName", new AttributeMapper<>(GroupModel::setName, Group::setDisplayName)));
    }

    public GroupCoreModelSchema() {
        super(Group.SCHEMA, ATTRIBUTE_MAPPERS);
    }

    @Override
    public String getName() {
        return Group.SCHEMA;
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
    protected String getAttributeSchema(GroupModel model, String name) {
        return "urn:ietf:params:scim:schemas:core:2.0:Group";
    }

    @Override
    protected String getAttributeSchemaName(GroupModel model, String name) {
        if (name.equals("name")) {
            return "displayName";
        }
        return null;
    }
}
