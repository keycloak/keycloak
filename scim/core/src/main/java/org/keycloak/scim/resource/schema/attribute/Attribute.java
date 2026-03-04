package org.keycloak.scim.resource.schema.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.keycloak.common.util.TriConsumer;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.common.MultiValuedAttribute;
import org.keycloak.scim.resource.schema.ModelSchema;

/**
 * Represents an attribute from a {@link ModelSchema}, its metadata and the mapper
 * that is used to map the attribute from a {@link ResourceTypeRepresentation} to a {@link Model} and vice versa.
 *
 * @see ModelSchema
 */
public class Attribute<M extends Model, R extends ResourceTypeRepresentation> {

    private final String alias;
    private BiFunction<KeycloakSession, Attribute<M, R>, String> modelAttributeResolver;
    private boolean primary;
    private String type;
    private String mutability;

    /**
     * Creates a simple attribute with the given {@code name}.
     *
     * @param name the name of the attribute from the {@link R} representation. It should be a simple attribute, meaning that it is not a complex attribute and does not have sub-attributes.
     * @return the builder
     */
    public static <M extends Model, R extends ResourceTypeRepresentation> Builder<M, R> simple(String name) {
        return new Builder<>(name, null);
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
        return new Builder<>(name, complexType);
    }

    /**
     * <p>Creates a complex attribute with the given {@code name} as a {@link MultiValuedAttribute} attribute. There is no
     * need to define sub-attributes for this type of attribute as they will be automatically mapped from {@link MultiValuedAttribute}.
     *
     * @param name the name of the attribute from the {@link R} representation.
     * @return the builder
     */
    public static <M extends Model, R extends ResourceTypeRepresentation> Builder<M, R> complex(String name, TriConsumer<M, String, String> modelSetter, BiFunction<KeycloakSession, Attribute<M, R>, String> resolver, boolean primary) {
        return (Builder<M, R>) new Builder<>(name, MultiValuedAttribute.class)
                .primary(primary)
                .modelAttributeResolver((session, attribute) -> resolver.apply(session, (Attribute<M, R>) attribute))
                .withSetters((TriConsumer<Model, String, String>) modelSetter)
                .withAttribute("value", name, (TriConsumer<Model, String, String>) modelSetter, primary)
                .withAttribute("primary", (model, subName, value) -> {return;});
    }

    private final String name;
    private final AttributeMapper<M, R> mapper;
    private String parentName;

    private Attribute(String name, AttributeMapper<M, R> mapper, String parentName) {
        this(name, mapper, parentName, null);
    }

    private Attribute(String name, AttributeMapper<M, R> mapper, String parentName, String alias) {
        this.name = name;
        this.mapper = mapper;
        this.parentName = parentName;
        this.alias = alias;
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
     * The mapper that is used to map the attribute from a {@link ResourceTypeRepresentation} to a {@link Model} and vice versa.
     *
     * @return the mapper
     */
    public AttributeMapper<M, R> getMapper() {
        return mapper;
    }

    /**
     * Returns if this attribute is a parent attribute of the given {@code resolved} attribute.
     * A parent attribute is usually an attribute that has sub-attributes, meaning that the name of the parent attribute is a
     * prefix of the name of the resolved attribute.
     *
     * @param sub the sub attribute to check if this attribute is its parent
     * @return {@code true} if this attribute is a parent attribute of the given {@code resolved} attribute. Otherwise, {@code false}
     */
    public boolean isParent(Attribute<M, R> sub) {
        return name.equals(sub.parentName);
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
     * @param session the session
     * @return the name of the attribute from the {@link Model} associated with this attribute or {@code null} if there is no mapping to this attribute
     */
    public String getModelAttributeName(KeycloakSession session) {
        if (modelAttributeResolver != null) {
            return modelAttributeResolver.apply(session, this);
        }
        return null;
    }

    private void setModelAttributeResolver(BiFunction<KeycloakSession, Attribute<M, R>, String> resolver) {
        this.modelAttributeResolver = resolver;
    }

    public boolean isPrimary() {
        return primary;
    }

    private void setPrimary(boolean primary) {
        this.primary = primary;
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

    private void setMutability(String mutability) {
        this.mutability = mutability;
    }

    public boolean isImmutable() {
        return Objects.equals(mutability, "immutable");
    }

    public static class Builder<M extends Model, R extends ResourceTypeRepresentation> {

        private final Class<?> complexType;
        private final String name;
        private TriConsumer<M, String, String> modelSetter;
        private BiConsumer<R, String> representationSetter;
        List<Attribute<M, R>> attributes = new ArrayList<>();
        private BiFunction<KeycloakSession, Attribute<M, R>, String> modelAttributeResolver;
        private boolean primary;
        private String type;
        private String mutability;

        private Builder(String name, Class<?> complexType) {
            Objects.requireNonNull(name, "name cannot be null");
            this.complexType = complexType;
            this.name = name;
        }

        public <C extends BiConsumer<R, String>> Builder<M, R> withSetters(TriConsumer<M, String, String> modelSetter, C representationSetter) {
            this.modelSetter = modelSetter;
            this.representationSetter = representationSetter;
            return this;
        }

        public Builder<M, R> withSetters(TriConsumer<M, String, String> modelSetter) {
            this.modelSetter = modelSetter;
            this.representationSetter = new ComplexAttributeSetter<>(name, complexType);
            return this;
        }

        public Builder<M, R> withSetters(BiConsumer<M, String> modelSetter) {
            this.modelSetter = (model, name, value) -> modelSetter.accept(model, value);
            this.representationSetter = new ComplexAttributeSetter<>(name, complexType);
            return this;
        }

        public Builder<M, R> withAttribute(String name, TriConsumer<M, String, String> modelSetter) {
            return withAttribute(name, null, modelSetter);
        }

        public Builder<M, R> withAttribute(String name, TriConsumer<M, String, String> modelSetter, boolean primary) {
            return withAttribute(name, null, modelSetter, primary);
        }

        public Builder<M, R> withAttribute(String name, String alias, TriConsumer<M, String, String> modelSetter) {
            return withAttribute(name, alias, modelSetter, false);
        }

        public Builder<M, R> withAttribute(String name, String alias, TriConsumer<M, String, String> modelSetter, boolean primary) {
            String subName = this.name + "." + name;
            Attribute<M, R> attribute = new Attribute<>(subName, new AttributeMapper<>(modelSetter, new ComplexAttributeSetter<>(this.name, name, complexType)), this.name, alias);
            attribute.setModelAttributeResolver(modelAttributeResolver);
            attribute.setPrimary(primary);
            attributes.add(attribute);
            return this;
        }

        public Builder<M, R> modelAttributeResolver(BiFunction<KeycloakSession, Attribute<M, R>, String> resolver) {
            this.modelAttributeResolver = resolver;
            return this;
        }

        public Builder<M, R> primary() {
            return primary(true);
        }

        private Builder<M, R> primary(boolean primary) {
            this.primary = primary;
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
            Attribute<M, R> attribute = new Attribute<>(name, new AttributeMapper<>(modelSetter, representationSetter), this.name);
            attribute.setModelAttributeResolver(modelAttributeResolver);
            attribute.setPrimary(primary);
            attribute.setType(type);
            attribute.setMutability(mutability);
            attributes.add(attribute);
            return attributes;
        }
    }
}
