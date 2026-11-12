/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.scim.model.group;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.scim.model.group.GroupAttributeMappings.Mapping;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.schema.AbstractModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;

/**
 * <p>A schema that exposes group attributes as attributes of SCIM Group extension schemas.
 *
 * <p>The attributes are configured per realm using {@link GroupAttributeMappings}. Each mapped attribute belongs to the
 * extension schema identified by the URN prefix of its SCIM name, so a single instance of this schema can serve
 * multiple extension schemas. The schema itself is internal and only the extension schemas with at least one mapped
 * attribute are advertised.
 */
public class GroupExtensionModelSchema extends AbstractModelSchema<GroupModel, Group> {

    public static final String KEYCLOAK_GROUP_SCHEMA = "urn:keycloak:params:scim:schemas:extension:realm:1.0:Group";

    private final KeycloakSession session;
    private Map<String, Mapping> mappings;

    public GroupExtensionModelSchema(KeycloakSession session) {
        super(KEYCLOAK_GROUP_SCHEMA);
        this.session = session;
    }

    @Override
    public String getName() {
        return "RealmGroup";
    }

    @Override
    public String getDescription() {
        return "Realm Group";
    }

    @Override
    public boolean isCore() {
        return false;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public boolean supports(Set<String> schemas) {
        if (schemas == null) {
            return false;
        }

        for (Attribute<GroupModel, Group> attribute : getAttributes().values()) {
            String schema = attribute.getSchema();

            if (schema != null && schemas.contains(schema)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected Set<String> getModelAttributeNames() {
        return getMappings().keySet();
    }

    @Override
    protected String getAttributeSchemaName(String name) {
        Mapping mapping = getMappings().get(name);
        return mapping == null ? null : mapping.scimAttribute();
    }

    @Override
    protected Object getAttributeValue(GroupModel model, String name) {
        Mapping mapping = getMappings().get(name);

        if (mapping == null) {
            return null;
        }

        if (mapping.multivalued()) {
            return model.getAttributeStream(name).toList();
        }

        return model.getFirstAttribute(name);
    }

    @Override
    protected Attribute<GroupModel, Group> getAttributeMapperByModelAttribute(String name) {
        Mapping mapping = getMappings().get(name);

        if (mapping == null) {
            return null;
        }

        return createAttribute(mapping);
    }

    private Map<String, Mapping> getMappings() {
        if (mappings == null) {
            mappings = GroupAttributeMappings.getMappings(session.getContext().getRealm());
        }
        return mappings;
    }

    private Attribute<GroupModel, Group> createAttribute(Mapping mapping) {
        Attribute.Builder<GroupModel, Group> builder = Attribute.<GroupModel, Group>simple(mapping.scimAttribute());

        if (mapping.readOnly()) {
            builder.readOnly();
        }

        if (mapping.multivalued()) {
            builder.multivalued()
                    .withModelRemover((GroupModel model, String name, Set<Object> values) -> {
                        if (mapping.readOnly()) {
                            return;
                        }
                        if (values == null || values.isEmpty()) {
                            model.removeAttribute(name);
                            return;
                        }
                        Set<String> removed = toStrings(values);
                        List<String> remaining = model.getAttributeStream(name)
                                .filter(value -> removed.stream().noneMatch(value::equalsIgnoreCase))
                                .toList();
                        if (remaining.isEmpty()) {
                            model.removeAttribute(name);
                        } else {
                            model.setAttribute(name, remaining);
                        }
                    })
                    .withModelAdder((GroupModel model, String name, Set<Object> values) -> {
                        if (mapping.readOnly() || values == null || values.isEmpty()) {
                            return;
                        }
                        Set<String> merged = new LinkedHashSet<>();
                        model.getAttributeStream(name).forEach(merged::add);
                        merged.addAll(toStrings(values));
                        model.setAttribute(name, List.copyOf(merged));
                    });
        }

        return builder
                .modelAttributeResolver(attribute -> mapping.name())
                .withSetters((model, name, value) -> {
                    // read-only attributes are ignored when set by SCIM clients, the same as for user attributes
                    if (mapping.readOnly()) {
                        return;
                    }
                    if (value == null) {
                        model.removeAttribute(name);
                    } else if (value instanceof Collection<?> values) {
                        if (values.isEmpty()) {
                            model.removeAttribute(name);
                        } else {
                            model.setAttribute(name, List.copyOf(toStrings(values)));
                        }
                    } else {
                        model.setSingleAttribute(name, value.toString());
                    }
                }, (attribute, group, value) -> {
                    if (value == null || (value instanceof Collection<?> values && values.isEmpty())) {
                        return;
                    }

                    String schema = attribute.getSchema();
                    Map<String, Object> extensions = group.getExtensions();

                    if (extensions == null) {
                        extensions = new HashMap<>();
                        group.setExtensions(extensions);
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> schemaAttributes = (Map<String, Object>) extensions.computeIfAbsent(schema, k -> new HashMap<>());

                    schemaAttributes.put(attribute.getSimpleName(), value);
                    group.addSchema(schema);
                }).build().get(0);
    }

    private static Set<String> toStrings(Collection<?> values) {
        Set<String> result = new LinkedHashSet<>();

        for (Object value : values) {
            if (value != null) {
                result.add(value.toString());
            }
        }

        return result;
    }
}
