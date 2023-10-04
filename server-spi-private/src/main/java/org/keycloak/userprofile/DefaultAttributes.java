/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.userprofile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;

/**
 * <p>The default implementation for {@link Attributes}. Should be reused as much as possible by the different implementations
 * of {@link UserProfileProvider}.
 *
 * <p>One of the main aspects of this implementation is to allow normalizing attributes accordingly to the profile
 * configuration and current context. As such, it provides some common normalization to common profile attributes (e.g.: username,
 * email, first and last names, dynamic read-only attributes).
 *
 * <p>This implementation is not specific to any user profile implementation.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultAttributes extends HashMap<String, List<String>> implements Attributes {

    /**
     * To reference dynamic attributes that can be configured as read-only when setting up the provider.
     * We should probably remove that once we remove the legacy provider, because this will come from the configuration.
     */
    public static final String READ_ONLY_ATTRIBUTE_KEY = "kc.read.only";

    protected final UserProfileContext context;
    protected final KeycloakSession session;
    private final Map<String, AttributeMetadata> metadataByAttribute;
    protected final UserModel user;

    public DefaultAttributes(UserProfileContext context, Map<String, ?> attributes, UserModel user,
            UserProfileMetadata profileMetadata,
            KeycloakSession session) {
        this.context = context;
        this.user = user;
        this.session = session;
        this.metadataByAttribute = configureMetadata(profileMetadata.getAttributes());
        putAll(Collections.unmodifiableMap(normalizeAttributes(attributes)));
    }

    @Override
    public boolean isReadOnly(String attributeName) {
        if (UserModel.USERNAME.equals(attributeName)) {
            if (isServiceAccountUser()) {
                return true;
            }
        }

        if (UserModel.EMAIL.equals(attributeName)) {
            if (isServiceAccountUser()) {
                return false;
            }
        }

        if (isReadOnlyFromMetadata(attributeName) || isReadOnlyInternalAttribute(attributeName)) {
            return true;
        }

        return getMetadata(attributeName) == null;
    }

    /**
     * Checks whether an attribute is marked as read only by looking at its metadata.
     *
     * @param attributeName the attribute name
     * @return @return {@code true} if the attribute is readonly. Otherwise, returns {@code false}
     */
    protected boolean isReadOnlyFromMetadata(String attributeName) {
        AttributeMetadata attributeMetadata = metadataByAttribute.get(attributeName);

        if (attributeMetadata == null) {
            return false;
        }

        return attributeMetadata.isReadOnly(createAttributeContext(attributeMetadata));
    }

    @Override
    public boolean isRequired(String name) {
        AttributeMetadata attributeMetadata = metadataByAttribute.get(name);

        if (attributeMetadata == null) {
            return false;
        }

        return attributeMetadata.isRequired(createAttributeContext(attributeMetadata));
    }

    @Override
    public boolean validate(String name, Consumer<ValidationError>... listeners) {
        Entry<String, List<String>> attribute = createAttribute(name);
        List<AttributeMetadata> metadatas = new ArrayList<>();

        metadatas.addAll(Optional.ofNullable(this.metadataByAttribute.get(attribute.getKey()))
                .map(Collections::singletonList).orElse(Collections.emptyList()));
        metadatas.addAll(Optional.ofNullable(this.metadataByAttribute.get(READ_ONLY_ATTRIBUTE_KEY))
                .map(Collections::singletonList).orElse(Collections.emptyList()));

        Boolean result = null;

        for (AttributeMetadata metadata : metadatas) {
            AttributeContext attributeContext = createAttributeContext(attribute, metadata);

            for (AttributeValidatorMetadata validator : metadata.getValidators()) {
                ValidationContext vc = validator.validate(attributeContext);

                if (vc.isValid()) {
                    continue;
                }

                if (result == null) {
                    result = false;
                }

                if (listeners != null) {
                    for (ValidationError error : vc.getErrors()) {
                        for (Consumer<ValidationError> consumer : listeners) {
                            consumer.accept(error);
                        }
                    }
                }
            }
        }

        return result == null;
    }

    @Override
    public List<String> getValues(String name) {
        return getOrDefault(name, EMPTY_VALUE);
    }

    @Override
    public boolean contains(String name) {
        return containsKey(name);
    }

    @Override
    public Set<String> nameSet() {
        return keySet();
    }

    @Override
    public Map<String, List<String>> getWritable() {
        Map<String, List<String>> attributes = new HashMap<>(this);

        for (String name : nameSet()) {
            AttributeMetadata metadata = getMetadata(name);
            RealmModel realm = session.getContext().getRealm();

            if (UserModel.USERNAME.equals(name)
                    && UserProfileContext.USER_API.equals(context)
                    && realm.isRegistrationEmailAsUsername()) {
                    continue;
            }

            if (metadata == null || !metadata.canEdit(createAttributeContext(metadata))) {
                attributes.remove(name);
            }
        }

        return attributes;
    }

    @Override
    public AttributeMetadata getMetadata(String name) {
        AttributeMetadata metadata = metadataByAttribute.get(name);

        if (metadata == null) {
            return null;
        }

        return metadata.clone();
    }

    @Override
    public Map<String, List<String>> getReadable() {
        Map<String, List<String>> attributes = new HashMap<>(this);

        for (String name : nameSet()) {
            AttributeMetadata metadata = getMetadata(name);

            if (metadata == null || !metadata.canView(createAttributeContext(metadata))) {
                attributes.remove(name);
            }
        }

        return attributes;
    }

    @Override
    public Map<String, List<String>> toMap() {
        return this;
    }

    protected boolean isServiceAccountUser() {
        return user != null && user.getServiceAccountClientLink() != null;
    }

    private AttributeContext createAttributeContext(Entry<String, List<String>> attribute, AttributeMetadata metadata) {
        return new AttributeContext(context, session, attribute, user, metadata);
    }

    private AttributeContext createAttributeContext(String attributeName, AttributeMetadata metadata) {
        return new AttributeContext(context, session, createAttribute(attributeName), user, metadata);
    }

    protected AttributeContext createAttributeContext(AttributeMetadata metadata) {
        return createAttributeContext(createAttribute(metadata.getName()), metadata);
    }

    private Map<String, AttributeMetadata> configureMetadata(List<AttributeMetadata> attributes) {
        Map<String, AttributeMetadata> metadatas = new HashMap<>();

        for (AttributeMetadata metadata : attributes) {
            // checks whether the attribute is selected for the current profile
            if (metadata.isSelected(createAttributeContext(metadata))) {
                metadatas.put(metadata.getName(), metadata);
            }
        }

        return metadatas;
    }

    private SimpleImmutableEntry<String, List<String>> createAttribute(String name) {
        return new SimpleImmutableEntry<String, List<String>>(name, null) {
            @Override
            public List<String> getValue() {
                List<String> values = get(name);

                if (values == null) {
                    return EMPTY_VALUE;
                }

                return values;
            }
        };
    }

    /**
     * Normalizes the given {@code attributes} (as they were provided when creating a profile) accordingly to the
     * profile configuration and the current context.
     *
     * @param attributes the denormalized map of attributes
     *
     * @return a normalized map of attributes
     */
    private Map<String, List<String>> normalizeAttributes(Map<String, ?> attributes) {
        Map<String, List<String>> newAttributes = new HashMap<>();
        RealmModel realm = session.getContext().getRealm();

        if (attributes != null) {
            for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                String key = entry.getKey();

                if (!isSupportedAttribute(key)) {
                    continue;
                }

                if (key.startsWith(Constants.USER_ATTRIBUTES_PREFIX)) {
                    key = key.substring(Constants.USER_ATTRIBUTES_PREFIX.length());
                }

                Object value = entry.getValue();
                List<String> values;

                if (value instanceof String) {
                    values = Collections.singletonList((String) value);
                } else {
                    values = (List<String>) value;
                }

                newAttributes.put(key, Collections.unmodifiableList(values));
            }
        }

        // the profile should always hold all attributes defined in the config
        for (String attributeName : metadataByAttribute.keySet()) {
            if (!isSupportedAttribute(attributeName) || newAttributes.containsKey(attributeName)) {
                continue;
            }

            List<String> values = EMPTY_VALUE;
            AttributeMetadata metadata = metadataByAttribute.get(attributeName);

            if (user != null && isIncludeAttributeIfNotProvided(metadata)) {
                values = user.getAttributes().getOrDefault(attributeName, EMPTY_VALUE);
            }

            newAttributes.put(attributeName, values);
        }

        if (user != null) {
            List<String> username = newAttributes.getOrDefault(UserModel.USERNAME, Collections.emptyList());

            if (username.isEmpty() && isReadOnly(UserModel.USERNAME)) {
                setUserName(newAttributes, Collections.singletonList(user.getUsername()));
            }
        }

        List<String> email = newAttributes.getOrDefault(UserModel.EMAIL, Collections.emptyList());

        if (!email.isEmpty() && realm.isRegistrationEmailAsUsername()) {
            List<String> lowerCaseEmailList = email.stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            setUserName(newAttributes, lowerCaseEmailList);

            if (user != null && isReadOnly(UserModel.EMAIL)) {
                newAttributes.put(UserModel.EMAIL, Collections.singletonList(user.getEmail()));
                setUserName(newAttributes, Collections.singletonList(user.getEmail()));
            }
        }

        return newAttributes;
    }

    private void setUserName(Map<String, List<String>> newAttributes, List<String> lowerCaseEmailList) {
        if (isServiceAccountUser()) {
            return;
        }
        newAttributes.put(UserModel.USERNAME, lowerCaseEmailList);
    }

    protected boolean isIncludeAttributeIfNotProvided(AttributeMetadata metadata) {
        return !metadata.canEdit(createAttributeContext(metadata));
    }

    /**
     * <p>Checks whether an attribute is support by the profile configuration and the current context.
     *
     * <p>This method can be used to avoid unexpected attributes from being added as an attribute because
     * the attribute source is a regular {@link Map} and not normalized.
     *
     * @param name the name of the attribute
     * @return
     */
    protected boolean isSupportedAttribute(String name) {
        if (READ_ONLY_ATTRIBUTE_KEY.equals(name)) {
            return false;
        }

        if (metadataByAttribute.containsKey(name)) {
            return true;
        }

        if (isServiceAccountUser()) {
            return true;
        }

        if (isReadOnlyInternalAttribute(name)) {
            return true;
        }

        // checks whether the attribute is a core attribute
        return isRootAttribute(name);
    }

    /**
     * <p>Returns whether an attribute is read only based on the provider configuration (using provider config),
     * usually related to internal attributes managed by the server.
     *
     * <p>For user-defined attributes, it should be preferable to use the user profile configuration.
     *
     * @param attributeName the attribute name
     * @return {@code true} if the attribute is readonly. Otherwise, returns {@code false}
     */
    protected boolean isReadOnlyInternalAttribute(String attributeName) {
        // read-only can be configured through the provider so we try to validate global validations
        AttributeMetadata readonlyMetadata = metadataByAttribute.get(READ_ONLY_ATTRIBUTE_KEY);

        if (readonlyMetadata == null) {
            return false;
        }

        AttributeContext attributeContext = createAttributeContext(attributeName, readonlyMetadata);

        for (AttributeValidatorMetadata validator : readonlyMetadata.getValidators()) {
            ValidationContext vc = validator.validate(attributeContext);
            if (!vc.isValid()) {
                return true;
            }
        }

        return false;
    }
}
