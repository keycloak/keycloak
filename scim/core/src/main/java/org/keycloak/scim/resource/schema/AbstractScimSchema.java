package org.keycloak.scim.resource.schema;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.keycloak.common.util.TriConsumer;
import org.keycloak.models.ModelIdentifier;
import org.keycloak.scim.resource.ScimResource;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.utils.JsonUtils.getJsonValue;

public abstract class AbstractScimSchema<M extends ModelIdentifier, R extends ScimResource> implements ScimModelSchema<M, R> {

    private final Map<String, AttributeMapper<M, R>> attributeMappers;

    protected AbstractScimSchema(Map<String, AttributeMapper<M, R>> attributeMappers) {
        this.attributeMappers = attributeMappers;
    }

    @Override
    public void populate(M model, R user) {
        try {
            populateModel(model, JsonSerialization.createObjectNode(user));
            user.setId(model.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void populate(R user, M model) {
        populateResourceType(user, model);
        user.setId(model.getId());
    }

    @Override
    public void validate(R resource) {

    }

    protected abstract Set<String> getAttributeNames(M model);
    protected abstract String getAttributeValue(M model, String name);
    protected abstract String getScimAttributeName(M model, String name);
    protected abstract String getScimSchema(M model, String name);

    private void populateModel(M model, ObjectNode objectNode) {
        for (String name : getAttributeNames(model)) {
            Object schema = getScimSchema(model, name);

            if (schema == null) {
                continue;
            }

            Object scimName = getScimAttributeName(model, name);

            if (scimName == null) {
                continue;
            }

            AttributeMapper<M, R> mapper = attributeMappers.get(scimName.toString());

            if (mapper != null) {
                Object value = getJsonValue(objectNode, scimName.toString());

                if (value == null) {
                    JsonNode schemaExtension = objectNode.get(schema.toString());
                    value = getJsonValue(schemaExtension, scimName.toString());
                }

                if (value != null) {
                    TriConsumer<M, String, String> setter = mapper.model.setter();

                    if (setter != null) {
                        setter.accept(model, name, value.toString());
                    }
                }
            }
        }
    }

    private void populateResourceType(R user, M model) {
        for (String name : getAttributeNames(model)) {
            Object schema = getScimSchema(model, name);

            if (schema == null) {
                continue;
            }

            Object fieldName = getScimAttributeName(model, name);

            if (fieldName == null) {
                continue;
            }

            AttributeMapper<M, R> mapper = attributeMappers.get(fieldName);

            if (mapper != null) {
                String value = getAttributeValue(model, name);
                BiConsumer<R, String> setter = mapper.resourceType.setter();

                if (setter != null) {
                    setter.accept(user, value);
                }
            }
        }
    }

    public static class AttributeMapper<M extends ModelIdentifier, R extends ScimResource> {

        private final ModelAttributeMapper<M> model;
        protected final ResourceTypeAttributeMapper<R> resourceType;

        public AttributeMapper(ModelAttributeMapper<M> model, ResourceTypeAttributeMapper<R> resourceType) {
            this.model = model;
            this.resourceType = resourceType;
        }
    }

    public static class ModelAttributeMapper<M> {

        private final BiFunction<M, String, String> getter;
        private final TriConsumer<M, String, String> setter;

        public ModelAttributeMapper(BiFunction<M, String, String> getter, TriConsumer<M, String, String> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        public TriConsumer<M, String, String> setter() {
            return setter;
        }
    }

    public static class ResourceTypeAttributeMapper<R extends ScimResource> {

        private final BiConsumer<R, String> setter;

        public ResourceTypeAttributeMapper(BiConsumer<R, String> setter) {
            this.setter = setter;
        }

        public BiConsumer<R, String> setter() {
            return setter;
        }
    }
}
