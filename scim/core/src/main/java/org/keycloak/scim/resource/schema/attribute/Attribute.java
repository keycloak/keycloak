package org.keycloak.scim.resource.schema.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.keycloak.common.util.TriConsumer;
import org.keycloak.models.Model;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents an attribute from a {@link ModelSchema}, its metadata and the mapper
 * that is used to map the attribute from a {@link ResourceTypeRepresentation} to a {@link Model} and vice versa.
 *
 * @see ModelSchema
 */
public class Attribute<M extends Model, R extends ResourceTypeRepresentation> {

    private final String alias;
    private Function<Attribute<M, R>, String> modelAttributeResolver;
    private String type;
    private String mutability;
    private boolean multivalued;
    private Class<?> complexType;

    /**
     * Creates a simple attribute with the given {@code name}.
     *
     * @param name the name of the attribute from the {@link R} representation. It should be a simple attribute, meaning that it is not a complex attribute and does not have sub-attributes.
     * @return the builder
     */
    public static <M extends Model, R extends ResourceTypeRepresentation> Builder<M, R> simple(String name) {
        return (Builder<M, R>) new Builder<>(name, null).string();
    }

    /**
     * <p>Creates a complex attribute with the given {@code name} and {@code complexType}.
     * <p>The {@code complexType} is used to determine the type of the complex attribute and to create the corresponding setter
     * for the representation.
     *
     * @param name the name of the attribute from the {@link R} representation. It should be a complex attribute, meaning that it has sub-attributes.
     * @param complexType the type of the complex attribute.
     * @return the builder
     */
    public static <M extends Model, R extends ResourceTypeRepresentation> Builder<M, R> complex(String name, Class<?> complexType) {
        Builder<M, R> builder = new Builder<>(name, complexType);
        builder.type = "complex";
        return builder;
    }

    private final String name;
    private final AttributeMapper<M, R> mapper;
    private final String parentName;

    private Attribute(String name, AttributeMapper<M, R> mapper, String parentName) {
        this(name, mapper, parentName, null);
    }

    private Attribute(String name, AttributeMapper<M, R> mapper, String parentName, String alias) {
        this.name = name;
        this.mapper = mapper;
        this.parentName = parentName;
        this.alias = alias;
        this.mapper.setAttribute(this);
    }

    /**
     * The name of the attribute from the {@link R} representation.
     *
     * @return the name of the attribute
     */
    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    /**
     * Returns the name of the parent attribute if this attribute is a sub-attribute. Otherwise, returns {@code null}.
     *
     * @return the name of the parent attribute or {@code null} if this attribute is not a sub-attribute
     */
    public String getParentName() {
        return parentName;
    }

    /**
     * Returns the name of the attribute from the {@link Model} associated with this attribute.
     *
     * @return the name of the attribute from the {@link Model} associated with this attribute or {@code null} if there is no mapping to this attribute
     */
    public String getModelAttributeName() {
        if (modelAttributeResolver != null) {
            return modelAttributeResolver.apply(this);
        }
        return null;
    }

    private void setModelAttributeResolver(Function<Attribute<M, R>, String> resolver) {
        this.modelAttributeResolver = resolver;
    }

    public boolean isTimestamp() {
        return Objects.equals(type, "timestamp");
    }

    public boolean isBoolean() {
        return Objects.equals(type, "boolean");
    }

    private void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    private void setMutability(String mutability) {
        this.mutability = mutability;
    }

    public boolean isImmutable() {
        return Objects.equals(mutability, "immutable");
    }

    private void setMultivalued(boolean multivalued) {
        this.multivalued = multivalued;
    }

    public boolean isMultivalued() {
        return multivalued;
    }

    private void setComplexType(Class<?> complexType) {
        this.complexType = complexType;
    }

    public Class<?> getComplexType() {
        return complexType;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Attribute<?, ?> attribute)) return false;
        return Objects.equals(name, attribute.name) && Objects.equals(parentName, attribute.parentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parentName);
    }

    public void set(M model, JsonNode value) {
        mapper.setValue(model, value);
    }

    public void set(R resource, Object value) {
        mapper.setValue(resource, value);
    }

    public void add(M model, JsonNode value) {
        mapper.addValue(model, value);
    }

    public void remove(M model, JsonNode value) {
        mapper.removeValue(model, value);
    }

    public static class Builder<M extends Model, R extends ResourceTypeRepresentation> {

        private final Class<?> complexType;
        private final String name;
        private TriConsumer<M, String, ?> modelSetter;
        private BiConsumer<R, ?> representationSetter;
        List<Attribute<M, R>> attributes = new ArrayList<>();
        private Function<Attribute<M, R>, String> modelAttributeResolver;
        private String type;
        private String mutability;
        private boolean multivalued;
        private TriConsumer<M, String, Set<?>> modelRemover;
        private TriConsumer<M, String, Set<?>> modelAdder;

        private Builder(String name, Class<?> complexType) {
            Objects.requireNonNull(name, "name cannot be null");
            this.complexType = complexType;
            this.name = name;
        }

        public <C extends BiConsumer<R, ?>> Builder<M, R> withModelSetter(TriConsumer<M, String, ?> modelSetter, C representationSetter) {
            this.modelSetter = modelSetter;
            this.representationSetter = representationSetter;
            return this;
        }

        public Builder<M, R> withModelSetter(TriConsumer<M, String, String> modelSetter) {
            this.modelSetter = modelSetter;
            this.representationSetter = new ComplexAttributeSetter<>(name, complexType);
            return this;
        }

        public Builder<M, R> withModelSetter(BiConsumer<M, String> modelSetter) {
            this.modelSetter = (model, name, value) -> modelSetter.accept(model, (String) value);
            this.representationSetter = new ComplexAttributeSetter<>(name, complexType);
            return this;
        }

        public Builder<M, R> withAttribute(String name, TriConsumer<M, String, String> modelSetter) {
            return withAttribute(name, null, modelSetter);
        }

        public Builder<M, R> withAttribute(String name, String alias, TriConsumer<M, String, String> modelSetter) {
            String subName = this.name + "." + name;
            Attribute<M, R> attribute = assembleAttribute(subName, this.name, alias,
                    new AttributeMapper<>(modelSetter, new ComplexAttributeSetter<>(this.name, name, complexType)),
                    modelAttributeResolver, "string", null, false, null);
            attributes.add(attribute);
            return this;
        }

        public Builder<M, R> modelAttributeResolver(Function<Attribute<M, R>, String> resolver) {
            this.modelAttributeResolver = resolver;
            return this;
        }

        public Builder<M, R> string() {
            this.type = "string";
            return this;
        }

        public Builder<M, R> timestamp() {
            this.type = "timestamp";
            return this;
        }

        public Builder<M, R> bool() {
            this.type = "boolean";
            return this;
        }

        public Builder<M, R> immutable() {
            this.mutability = "immutable";
            return this;
        }

        public List<Attribute<M, R>> build() {
            Attribute<M, R> attribute = assembleAttribute(name, null, null,
                    new AttributeMapper<>(modelSetter, representationSetter, modelRemover, modelAdder),
                    modelAttributeResolver, type, mutability, multivalued, complexType);
            if (attributes.isEmpty()) {
                // do not add the root attribute if there are subattributes
                attributes.add(attribute);
            }
            return attributes;
        }

        private Attribute<M, R> assembleAttribute(String name, String parentName, String alias,
                                                   AttributeMapper<M, R> mapper,
                                                   Function<Attribute<M, R>, String> modelAttributeResolver,
                                                   String type, String mutability,
                                                   boolean multivalued, Class<?> complexType) {
            Attribute<M, R> attribute = new Attribute<>(name, mapper, parentName, alias);
            attribute.setModelAttributeResolver(modelAttributeResolver);
            attribute.setType(type);
            attribute.setMutability(mutability);
            attribute.setMultivalued(multivalued);
            attribute.setComplexType(complexType);
            return attribute;
        }

        public Builder<M, R> multivalued() {
            this.multivalued = true;
            return this;
        }

        public <C> Builder<M, R> withModelRemover(TriConsumer<M, String, Set<C>> remover) {
            this.modelRemover = (m, s, objects) -> remover.accept(m, s, (Set<C>) objects);
            return this;
        }

        public <C> Builder<M, R> withModelAdder(TriConsumer<M, String, Set<C>> adder) {
            this.modelAdder = (m, s, objects) -> adder.accept(m, s, (Set<C>) objects);
            return this;
        }
    }
}
