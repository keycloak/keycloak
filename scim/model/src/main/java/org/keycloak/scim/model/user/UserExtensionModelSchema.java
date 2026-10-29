package org.keycloak.scim.model.user;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.User;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;

import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;
import static org.keycloak.scim.resource.Scim.USER_CORE_SCHEMA;
import static org.keycloak.userprofile.UserProfileUtil.isRootAttribute;

public class UserExtensionModelSchema extends AbstractUserModelSchema {

    public static final String KEYCLOAK_USER_SCHEMA = "urn:keycloak:params:scim:schemas:extension:realm:1.0:User";

    public UserExtensionModelSchema(KeycloakSession session) {
        this(session, KEYCLOAK_USER_SCHEMA);
    }

    public UserExtensionModelSchema(KeycloakSession session, String schema) {
        super(session, schema);
    }

    @Override
    public String getName() {
        return "RealmUser";
    }

    @Override
    public String getDescription() {
        return "Realm User";
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
        for (Attribute<UserModel, User> value : getAttributes().values()) {
            String schema = value.getSchema();

            if (schema != null && schemas.contains(schema)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected Set<String> getModelAttributeNames() {
        if (isCore()) {
            return Set.of();
        }

        Set<String> names = new HashSet<>();
        UserProfile profile = getUserProfile();
        Attributes attributes = profile.getAttributes();

        for (String name : attributes.getReadable().keySet()) {
            if (isRootAttribute(name)) {
                continue;
            }

            AttributeMetadata metadata = attributes.getMetadata(name);

            if (metadata == null) {
                continue;
            }

            String scimName = (String) metadata.getAnnotations().get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE);

            if (scimName == null || !scimName.contains(":")) {
                continue;
            }

            names.add(name);
        }

        return names;
    }

    @Override
    protected Attribute<UserModel, User> getAttributeMapperByModelAttribute(String name) {
        UserProfile profile = getUserProfile();
        Attributes attributes = profile.getAttributes();
        AttributeMetadata metadata = attributes.getMetadata(name);

        if (metadata == null) {
            return null;
        }

        Map<String, Object> annotations = metadata.getAnnotations();

        if (annotations == null) {
            return null;
        }

        String scimName = (String) annotations.get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE);

        if (scimName == null) {
            return null;
        }

        if (!hasSchema(scimName)) {
            return null;
        }

        return createCustomAttribute(scimName, metadata.isMultivalued());
    }

    @Override
    protected boolean hasSchema(String attributeName) {
        String schema = Attribute.getSchema(attributeName);

        // it should be possible to query other schemas from the providers
        return schema != null && !List.of(USER_CORE_SCHEMA, ENTERPRISE_USER_SCHEMA).contains(schema);
    }

    private Attribute<UserModel, User> createCustomAttribute(Object scimName, boolean isMultivalued) {
        Attribute.Builder<UserModel, User> builder = Attribute.<UserModel, User>simple(scimName.toString());
        if (isMultivalued) {
            builder = builder.multivalued()
                    .withModelRemover((model, name, values) -> {
                        if (getUserProfile().getAttributes().isReadOnly(name)) {
                            return;
                        }
                        if (values == null || values.isEmpty()) {
                            model.removeAttribute(name);
                            return;
                        }
                        List<String> remaining = model.getAttributeStream(name)
                                .filter(value -> !values.contains(value))
                                .toList();
                        if (remaining.isEmpty()) {
                            model.removeAttribute(name);
                        } else {
                            model.setAttribute(name, remaining);
                        }
                    })
                    .withModelAdder((model, name, values) -> {
                        if (getUserProfile().getAttributes().isReadOnly(name) || values == null || values.isEmpty()) {
                            return;
                        }
                        Set<String> merged = new LinkedHashSet<>();
                        model.getAttributeStream(name).forEach(merged::add);
                        merged.addAll(values.stream().map(Object::toString).toList());
                        model.setAttribute(name, List.copyOf(merged));
                    });
        }

        return builder
                .modelAttributeResolver(attribute -> {
                    if (isCore()) {
                        return null;
                    }
                    UserProfile profile = getUserProfile();
                    Attributes attributes = profile.getAttributes();

                    for (String modelName : attributes.getReadable().keySet()) {
                        AttributeMetadata metadata = attributes.getMetadata(modelName);

                        if (metadata == null) {
                            return null;
                        }

                        Map<String, Object> annotations = metadata.getAnnotations();

                        if (annotations == null) {
                            return null;
                        }

                        Object modelScimAttributeName = annotations.get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE);

                        if (attribute.getName().equals(modelScimAttributeName)) {
                            return modelName;
                        }
                    }

                    return null;
                })
                .withSetters((model, name, value) -> {
                    if (isCore()) {
                        return;
                    }
                    if (getAttributeMapperByModelAttribute(name) == null) {
                        return;
                    }
                    if (getUserProfile().getAttributes().isReadOnly(name)) {
                        return;
                    }
                    if (value == null) {
                        model.removeAttribute(name);
                    } else if (value instanceof Collection<?> values) {
                        model.setAttribute(name, values.stream().map(Object::toString).toList());
                    } else {
                        model.setSingleAttribute(name, value.toString());
                    }
                }, (attribute, user, value) -> {
                    if (isCore()) {
                        return;
                    }

                    if (value == null || (value instanceof Collection<?> collection && collection.isEmpty()) || attribute.getSchema() == null) {
                        return;
                    }

                    String schema = attribute.getSchema();

                    String attributeName = attribute.getSimpleName();
                    Map<String, Object> extensions = user.getExtensions();

                    if (extensions == null) {
                        extensions = new HashMap<>();
                        user.setExtensions(extensions);
                    }

                    Map<String, Object> subAttributes = (Map<String, Object>) extensions.computeIfAbsent(schema, k -> new HashMap<>());

                    user.addSchema(schema);

                    int subSubAttribute = attributeName.indexOf('.');

                    if (subSubAttribute != -1) {
                        String parentAttributeName = attributeName.substring(0, subSubAttribute);
                        attributeName = attributeName.substring(parentAttributeName.length() + 1);
                        Object existingParent = subAttributes.get(parentAttributeName);
                        // Handle multivalued attributes annotated as "<parent>.value"
                        // so that SCIM gets: "<parent>": [ { "value": "..." }, ... ]
                        if ("value".equals(attributeName) && value instanceof Collection<?> values) {
                            if (existingParent instanceof Map<?, ?> existingMap && !existingMap.isEmpty()) {
                                throw incompatibleSiblingMapping(parentAttributeName, schema);
                            }
                            subAttributes.put(parentAttributeName, values.stream()
                                    .filter(Objects::nonNull)
                                    .map(v -> Map.<String, Object>of("value", v))
                                    .toList());
                            return;
                        }
                        if (existingParent instanceof Collection<?>) {
                            throw incompatibleSiblingMapping(parentAttributeName, schema);
                        }
                        subAttributes = (Map<String, Object>) subAttributes.computeIfAbsent(parentAttributeName, k -> new HashMap<>());
                    }

                    subAttributes.put(attributeName, value);
                }).build().get(0);
    }

    private ModelValidationException incompatibleSiblingMapping(String parentAttributeName, String schema) {
        return new ModelValidationException(
                "Incompatible SCIM extension mappings for complex attribute '" + schema + ":" + parentAttributeName
                        + "': multivalued '.value' cannot be combined with sibling sub-attributes");
    }
}
