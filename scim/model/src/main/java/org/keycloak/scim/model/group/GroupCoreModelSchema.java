package org.keycloak.scim.model.group;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.models.GroupModel;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.schema.AbstractModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;

public final class GroupCoreModelSchema extends AbstractModelSchema<GroupModel, Group> {

    public GroupCoreModelSchema() {
        super(Group.SCHEMA);
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
    protected String getAttributeSchemaName(GroupModel model, String name) {
        if (name.equals("name")) {
            return "displayName";
        }
        return null;
    }

    @Override
    protected Map<String, Attribute<GroupModel, Group>> doGetAttributes() {
        return new ArrayList<>((Attribute.<GroupModel, Group>simple("displayName")
                    .primary()
                    .modelAttributeResolver((session, attribute) -> {
                        if (attribute.getName().equals("displayName")) {
                            return "name";
                        }
                        return null;
                    })
                    .withSetters(GroupModel::setName)
                    .build())).stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }
}
