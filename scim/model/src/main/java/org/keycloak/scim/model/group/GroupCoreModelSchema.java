package org.keycloak.scim.model.group;

import java.util.ArrayList;
import java.util.List;
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
    public String getId() {
        return Group.SCHEMA;
    }

    @Override
    public String getName() {
        return "Group";
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    protected Set<String> getModelAttributeNames() {
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
    protected String getAttributeSchemaName(String name) {
        if (name.equals("name")) {
            return "displayName";
        }
        return null;
    }

    @Override
    protected Map<String, Attribute<GroupModel, Group>> doGetAttributes() {
        List<Attribute<GroupModel, Group>> attributes = new ArrayList<>(Attribute.<GroupModel, Group>simple("displayName")
                    .modelAttributeResolver((attribute) -> {
                        if (attribute.getName().equals("displayName")) {
                            return "name";
                        }
                        return null;
                    })
                    .withModelSetter(GroupModel::setName)
                    .build());
        attributes.addAll(Attribute.<GroupModel, Group>simple("meta.created")
                .timestamp()
                .immutable()
                .modelAttributeResolver(attribute -> "createdTimestamp")
                .build());
        attributes.addAll(Attribute.<GroupModel, Group>simple("meta.lastModified")
                .timestamp()
                .modelAttributeResolver(attribute -> "lastModifiedTimestamp")
                .build());
        return attributes.stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }

    @Override
    public void populate(Group resource, GroupModel model) {
        super.populate(resource, model);
        Long createdTimestamp = model.getCreatedTimestamp();
        if (createdTimestamp != null) {
            resource.setCreatedTimestamp(createdTimestamp);
        }
        Long lastModified = model.getLastModifiedTimestamp();
        if (lastModified != null) {
            resource.setLastModifiedTimestamp(lastModified);
        }
    }
}
