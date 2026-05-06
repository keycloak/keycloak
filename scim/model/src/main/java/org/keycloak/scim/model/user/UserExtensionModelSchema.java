package org.keycloak.scim.model.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
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

    private final KeycloakSession session;

    public UserExtensionModelSchema(KeycloakSession session) {
        this(session, KEYCLOAK_USER_SCHEMA);
    }

    public UserExtensionModelSchema(KeycloakSession session, String schema) {
        super(session, schema);
        this.session = session;
    }

    @Override
    public String getId() {
        return KEYCLOAK_USER_SCHEMA;
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

        for (String name : profile.getAttributes().nameSet()) {
            if (isRootAttribute(name)) {
                continue;
            }

            AttributeMetadata metadata = profile.getAttributes().getMetadata(name);

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

        return createCustomAttribute(scimName);
    }

    @Override
    protected boolean hasSchema(String attributeName) {
        String schema = Attribute.getSchema(attributeName);

        // it should be possible to query other schemas from the providers
        return schema != null && !List.of(USER_CORE_SCHEMA, ENTERPRISE_USER_SCHEMA).contains(schema);
    }

    private Attribute<UserModel,  User> createCustomAttribute(Object scimName) {
        return Attribute.<UserModel, User>simple(scimName.toString())
                .modelAttributeResolver(attribute -> {
                    if (isCore()) {
                        return null;
                    }
                    UserProfile profile = getUserProfile();
                    Attributes attributes = profile.getAttributes();

                    for (String modelName : attributes.nameSet()) {
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
                    if (value == null) {
                        model.removeAttribute(name);
                    } else {
                        model.setSingleAttribute(name, value.toString());
                    }
                }, (attribute, user, value) -> {
                    if (isCore()) {
                        return;
                    }

                    String schema = attribute.getSchema();

                    if (schema == null) {
                        return;
                    }

                    String attributeName = attribute.getSimpleName();
                    Map<String, Object> extensions = user.getExtensions();

                    if (extensions == null) {
                        extensions = new HashMap<>();
                        user.setExtensions(extensions);
                    }

                    Map<String, Object> subAttributes = (Map<String, Object>) extensions.get(schema);

                    if (subAttributes == null) {
                        subAttributes = new HashMap<>();
                        extensions.put(schema, subAttributes);
                        user.addSchema(schema);
                    }

                    int subSubAttribute = attributeName.indexOf('.');

                    if (subSubAttribute != -1) {
                        String parentAttributeName = attributeName.substring(0, subSubAttribute);
                        subAttributes.put(parentAttributeName, new HashMap<>());
                        subAttributes = (Map<String, Object>) subAttributes.get(parentAttributeName);
                        attributeName = attributeName.substring(parentAttributeName.length() + 1);
                    }

                    subAttributes.put(attributeName, value);
                }).build().get(0);
    }
}
