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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.storage.StorageId;
import org.keycloak.utils.StringUtil;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.validators.LengthValidator;

import org.jboss.logging.Logger;

import static java.util.Collections.emptyList;

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

    private static final Logger logger = Logger.getLogger(DefaultAttributes.class);

    /**
     * To reference dynamic attributes that can be configured as read-only when setting up the provider.
     * We should probably remove that once we remove the legacy provider, because this will come from the configuration.
     */
    public static final String READ_ONLY_ATTRIBUTE_KEY = "kc.read.only";
    public static final String DEFAULT_MAX_LENGTH_ATTRIBUTES = "2048";

    protected final UserProfileContext context;
    protected final KeycloakSession session;
    private final Map<String, AttributeMetadata> metadataByAttribute;
    private final UPConfig upConfig;
    protected final UserModel user;
    private final Map<String, List<String>> unmanagedAttributes = new HashMap<>();

    public DefaultAttributes(UserProfileContext context, Map<String, ?> attributes, UserModel user,
            UserProfileMetadata profileMetadata,
            KeycloakSession session) {
        this.context = context;
        this.user = user;
        this.session = session;
        this.metadataByAttribute = configureMetadata(profileMetadata.getAttributes(), profileMetadata);
        this.upConfig = session.getProvider(UserProfileProvider.class).getConfiguration();
        putAll(Collections.unmodifiableMap(normalizeAttributes(attributes)));
    }

    @Override
    public boolean isReadOnly(String name) {
        if (isReadableOrWritableDuringRegistration(name)) {
            return false;
        }
        if (isReadOnlyFromMetadata(name) || isReadOnlyInternalAttribute(name)) {
            return true;
        }

        if (!isManagedAttribute(name)) {
            return !isAllowEditUnmanagedAttribute();
        }

        return getMetadata(name) == null;
    }

    private boolean isReadableOrWritableDuringRegistration(String name) {
        if (context.equals(UserProfileContext.REGISTRATION) && isRequired(name)) {
            // in context of registration, username or email (email as username) cannot be readonly otherwise registration is not possible
            if (UserModel.EMAIL.equals(name)) {
                RealmModel realm = session.getContext().getRealm();
                return realm.isRegistrationEmailAsUsername();
            }
            return UserModel.USERNAME.equals(name);
        }
        return false;
    }

    private boolean isAllowEditUnmanagedAttribute() {
        UnmanagedAttributePolicy unmanagedAttributesPolicy = upConfig.getUnmanagedAttributePolicy();

        if (!isAllowUnmanagedAttribute()) {
            return false;
        }

        switch (unmanagedAttributesPolicy) {
            case ENABLED:
                return true;
            case ADMIN_EDIT:
                return context.isAdminContext();
        }

        return false;
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
                .map(Collections::singletonList).orElse(emptyList()));
        metadatas.addAll(Optional.ofNullable(this.metadataByAttribute.get(READ_ONLY_ATTRIBUTE_KEY))
                .map(Collections::singletonList).orElse(emptyList()));
        addDefaultValidators(name, metadatas);

        Boolean result = null;

        for (AttributeMetadata metadata : metadatas) {
            AttributeContext attributeContext = createAttributeContext(attribute, metadata);

            for (AttributeValidatorMetadata validator : metadata.getValidators()) {
                ValidationContext vc = validator.validate(attributeContext);

                if (vc.isValid()) {
                    continue;
                }

                if (user != null && metadata.isReadOnly(attributeContext)) {
                    List<String> value = user.getAttributeStream(name).filter(StringUtil::isNotBlank).collect(Collectors.toList());
                    List<String> newValue = attribute.getValue().stream().filter(StringUtil::isNotBlank).collect(Collectors.toList());
                    if (CollectionUtil.collectionEquals(value, newValue)) {
                        // allow update if the value was already wrong in the user and is read-only in this context
                        logger.debugf("User '%s' attribute '%s' has previous validation errors %s but is read-only in context %s.",
                                user.getUsername(), name, vc.getErrors(), attributeContext.getContext());
                        continue;
                    }
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

    protected void addDefaultValidators(String name, List<AttributeMetadata> metadatas) {
        addLengthValidatorIfNotSet(name, metadatas);
    }

    /**
     * In case there are unmanaged attributes or attributes that don't have a length restrictions,
     * add a default length restriction to avoid a denial of service by a caller.
     */
    private void addLengthValidatorIfNotSet(String name, List<AttributeMetadata> metadatas) {
        for (AttributeMetadata metadata : metadatas) {
            for (AttributeValidatorMetadata validator : metadata.getValidators()) {
                if (validator.getValidatorId().equals(LengthValidator.ID)) {
                    return;
                }
            }
        }

        AttributeMetadata am = new AttributeMetadata(name, -1);
        Map<String, Object> vc = new HashMap<>();
        vc.put(LengthValidator.KEY_MIN, "0");
        vc.put(LengthValidator.KEY_MAX, DEFAULT_MAX_LENGTH_ATTRIBUTES);
        am.addValidators(Collections.singletonList(new AttributeValidatorMetadata(LengthValidator.ID, new ValidatorConfig(vc))));
        metadatas.add(am);
    }

    @Override
    public List<String> get(String name) {
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

            if ((UserModel.USERNAME.equals(name) && realm.isRegistrationEmailAsUsername())
                || isReadableOrWritableDuringRegistration(name)
                || !isManagedAttribute(name)) {
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
        if (unmanagedAttributes.containsKey(name)) {
            return createUnmanagedAttributeMetadata(name);
        }

        return Optional.ofNullable(metadataByAttribute.get(name))
                .map(AttributeMetadata::clone)
                .orElse(null);
    }

    @Override
    public Map<String, List<String>> getReadable() {
        Map<String, List<String>> attributes = new HashMap<>(this);

        for (String name : nameSet()) {
            AttributeMetadata metadata = getMetadata(name);

            if (metadata == null) {
                attributes.remove(name);
                continue;
            }

            if (isReadableOrWritableDuringRegistration(name)) {
                continue;
            }

            AttributeContext attributeContext = createAttributeContext(metadata);

            if (!metadata.canView(attributeContext) || !metadata.isSelected(attributeContext)) {
                attributes.remove(name);
            }
        }

        return attributes;
    }

    @Override
    public Map<String, List<String>> toMap() {
        return Collections.unmodifiableMap(this);
    }

    private AttributeContext createAttributeContext(Entry<String, List<String>> attribute, AttributeMetadata metadata) {
        return new AttributeContext(context, session, attribute, user, metadata, this);
    }

    private AttributeContext createAttributeContext(String attributeName, AttributeMetadata metadata) {
        return new AttributeContext(context, session, createAttribute(attributeName), user, metadata, this);
    }

    protected AttributeContext createAttributeContext(AttributeMetadata metadata) {
        return createAttributeContext(createAttribute(metadata.getName()), metadata);
    }

    private Map<String, AttributeMetadata> configureMetadata(List<AttributeMetadata> attributes, UserProfileMetadata profileMetadata) {
        Map<String, AttributeMetadata> metadatas = new HashMap<>();

        for (AttributeMetadata metadata : attributes) {
            // checks whether the attribute is selected for the current profile
            if (metadata.isSelected(createAttributeContext(metadata))) {
                metadatas.put(metadata.getName(), metadata);
            }
        }

        metadatas.putAll(getUserStorageProviderMetadata(profileMetadata));

        return metadatas;
    }

    private Map<String, AttributeMetadata> getUserStorageProviderMetadata(UserProfileMetadata profileMetadata) {
        if (user == null || (StorageId.isLocalStorage(user.getId()) && !user.isFederated())) {
            // new user or not a user from a storage provider other than local
            return Collections.emptyMap();
        }

        String providerId = user.getFederationLink();
        UserProvider userProvider = session.users();

        if (userProvider instanceof UserProfileDecorator) {
            // query the user provider from the source user storage provider for additional attribute metadata
            UserProfileDecorator decorator = (UserProfileDecorator) userProvider;
            return decorator.decorateUserProfile(providerId, profileMetadata).stream()
                    .collect(Collectors.toMap(AttributeMetadata::getName, Function.identity()));
        }

        return Collections.emptyMap();
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
                String name = entry.getKey();

                if (!isSupportedAttribute(name)) {
                    if (!isManagedAttribute(name) && isAllowUnmanagedAttribute()) {
                        String normalizedName = normalizeAttributeName(name);
                        unmanagedAttributes.put(normalizedName, normalizeAttributeValues(normalizedName, entry.getValue()));
                    }
                    continue;
                }

                String normalizedName = normalizeAttributeName(name);
                List<String> values = normalizeAttributeValues(normalizedName, entry.getValue());

                newAttributes.put(normalizedName, Collections.unmodifiableList(values));
            }
        }

        // the profile should always hold all attributes defined in the config
        for (var entry : metadataByAttribute.entrySet()) {
            String attributeName = entry.getKey();
            if (!isSupportedAttribute(attributeName) || newAttributes.containsKey(attributeName)) {
                continue;
            }

            List<String> values = EMPTY_VALUE;
            AttributeMetadata metadata = entry.getValue();

            if (user != null && isIncludeAttributeIfNotProvided(metadata)) {
                values = user.getAttributes().getOrDefault(attributeName, EMPTY_VALUE);
            }

            newAttributes.put(attributeName, normalizeAttributeValues(attributeName, values));
        }

        if (user != null) {
            List<String> username = newAttributes.getOrDefault(UserModel.USERNAME, emptyList());

            if (username.isEmpty() && isReadOnly(UserModel.USERNAME)) {
                setUserName(newAttributes, Collections.singletonList(user.getUsername()));
            }
        }

        List<String> email = newAttributes.getOrDefault(UserModel.EMAIL, emptyList());

        if (!email.isEmpty() && realm.isRegistrationEmailAsUsername()) {
            setUserName(newAttributes, email);

            if (user != null && isReadOnly(UserModel.EMAIL)) {
                newAttributes.put(UserModel.EMAIL, Collections.singletonList(user.getEmail()));
                setUserName(newAttributes, Collections.singletonList(user.getEmail()));
            }
        }

        if (isAllowUnmanagedAttribute()) {
            newAttributes.putAll(unmanagedAttributes);
        }

        return newAttributes;
    }

    private static String normalizeAttributeName(String name) {
        if (name.startsWith(Constants.USER_ATTRIBUTES_PREFIX)) {
            return name.substring(Constants.USER_ATTRIBUTES_PREFIX.length());
        }
        return name;
    }

    /**
     * Intentionally kept to protected visibility to allow for custom normalization logic while clients adopt User Profile
     */
    protected List<String> normalizeAttributeValues(String name, Object value) {
        List<String> values;

        if (value instanceof String) {
            values = Collections.singletonList((String) value);
        } else {
            values = value == null ? EMPTY_VALUE : (List<String>) value;
        }

        AttributeMetadata metadata = metadataByAttribute.get(name);

        if (values.isEmpty() && metadata != null && metadata.getDefaultValue() != null) {
            values = List.of(metadata.getDefaultValue());
        }

        Stream<String> valuesStream = values.stream().filter(Objects::nonNull);

        // do not normalize the username if a federated user because we need to respect the format from the external identity store
        if ((UserModel.USERNAME.equals(name) && !isFederated()) || UserModel.EMAIL.equals(name)) {
            valuesStream = valuesStream.map(KeycloakModelUtils::toLowerCaseSafe);
        }

        return valuesStream.collect(Collectors.toList());
    }

    protected boolean isAllowUnmanagedAttribute() {
        UnmanagedAttributePolicy unmanagedAttributePolicy = upConfig.getUnmanagedAttributePolicy();

        if (unmanagedAttributePolicy == null) {
            // unmanaged attributes disabled
            return false;
        }

        switch (unmanagedAttributePolicy) {
            case ADMIN_EDIT:
            case ADMIN_VIEW:
                // unmanaged attributes only available through the admin context
                return context.isAdminContext();
        }

        // allow unmanaged attributes if enabled to all contexts
        return UnmanagedAttributePolicy.ENABLED.equals(unmanagedAttributePolicy);
    }

    protected void setUserName(Map<String, List<String>> newAttributes, List<String> values) {
        newAttributes.put(UserModel.USERNAME, values);
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

        if (isManagedAttribute(name)) {
            return true;
        }

        return isReadOnlyInternalAttribute(name);
    }

    private boolean isManagedAttribute(String name) {
        return metadataByAttribute.containsKey(normalizeAttributeName(name));
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

    @Override
    public Map<String, List<String>> getUnmanagedAttributes() {
        return unmanagedAttributes;
    }

    @Override
    public Map<String, Object> getAnnotations(String name) {
        AttributeMetadata metadata = getMetadata(name);

        if (metadata == null) {
            return Collections.emptyMap();
        }

        AttributeContext context = createAttributeContext(metadata);

        return metadata.getAnnotations(context);
    }

    protected AttributeMetadata createUnmanagedAttributeMetadata(String name) {
        return new AttributeMetadata(name, Integer.MAX_VALUE) {
            final UnmanagedAttributePolicy unmanagedAttributePolicy = upConfig.getUnmanagedAttributePolicy();

            @Override
            public boolean canView(AttributeContext context) {
                return canEdit(context)
                        || (UnmanagedAttributePolicy.ADMIN_VIEW.equals(unmanagedAttributePolicy) && context.getContext().isAdminContext());
            }

            @Override
            public boolean canEdit(AttributeContext context) {
                return UnmanagedAttributePolicy.ENABLED.equals(unmanagedAttributePolicy)
                        || (UnmanagedAttributePolicy.ADMIN_EDIT.equals(unmanagedAttributePolicy) && context.getContext().isAdminContext());
            }
        };
    }

    private boolean isFederated() {
        return user != null && user.isFederated();
    }
}
