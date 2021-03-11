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

package org.keycloak.userprofile.legacy;

import static org.keycloak.userprofile.DefaultAttributes.READ_ONLY_ATTRIBUTE_KEY;
import static org.keycloak.userprofile.UserProfileContext.*;
import static org.keycloak.userprofile.UserProfileContext.ACCOUNT;
import static org.keycloak.userprofile.UserProfileContext.ACCOUNT_OLD;
import static org.keycloak.userprofile.UserProfileContext.IDP_REVIEW;
import static org.keycloak.userprofile.UserProfileContext.REGISTRATION_PROFILE;
import static org.keycloak.userprofile.UserProfileContext.UPDATE_PROFILE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.DefaultAttributes;
import org.keycloak.userprofile.DefaultUserProfile;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileMetadata;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.UserProfileProviderFactory;
import org.keycloak.userprofile.AttributeValidatorMetadata;
import org.keycloak.userprofile.validation.Validator;

/**
 * <p>A base class for {@link UserProfileProvider} implementations providing the main hooks for customizations.
 *
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public abstract class AbstractUserProfileProvider<U extends UserProfileProvider> implements UserProfileProvider, UserProfileProviderFactory<U> {

    private static final Logger logger = Logger.getLogger(DefaultAttributes.class);

    public static Pattern getRegexPatternString(String[] builtinReadOnlyAttributes) {
        if (builtinReadOnlyAttributes != null) {
            List<String> readOnlyAttributes = new ArrayList<>(Arrays.asList(builtinReadOnlyAttributes));

            String regexStr = readOnlyAttributes.stream()
                    .map(configAttrName -> configAttrName.endsWith("*")
                            ? "^" + Pattern.quote(configAttrName.substring(0, configAttrName.length() - 1)) + ".*$"
                            : "^" + Pattern.quote(configAttrName) + "$")
                    .collect(Collectors.joining("|"));
            regexStr = "(?i:" + regexStr + ")";

            return Pattern.compile(regexStr);
        }

        return null;
    }

    public static Validator isReadOnlyAttributeUnchanged(Pattern pattern) {
        return (context) -> {
            Map.Entry<String, List<String>> attribute = context.getAttribute();
            String key = attribute.getKey();

            if (!pattern.matcher(key).find()) {
                return true;
            }

            List<String> values = attribute.getValue();

            if (values == null) {
                return true;
            }

            UserModel user = context.getUser();

            List<String> existingAttrValues = user == null ? null : user.getAttribute(key);
            String existingValue = null;

            if (existingAttrValues != null && !existingAttrValues.isEmpty()) {
                existingValue = existingAttrValues.get(0);
            }

            if (values.isEmpty() && existingValue != null) {
                return false;
            }

            String value = null;

            if (!values.isEmpty()) {
                value = values.get(0);
            }

            boolean result = ObjectUtil.isEqualOrBothNull(value, existingValue);

            if (!result) {
                logger.warnf("Attempt to edit denied attribute '%s' of user '%s'", pattern, user == null ? "new user" : user.getFirstAttribute(UserModel.USERNAME));
            }

            return result;
        };
    }

    /**
     * There are the declarations for creating the built-in validations for read-only attributes. Regardless of the context where
     * user profiles are used. They are related to internal attributes with hard conditions on them in terms of management.
     */
    private static String UPDATE_READ_ONLY_ATTRIBUTES_REJECTED = "updateReadOnlyAttributesRejectedMessage";
    private static String[] DEFAULT_READ_ONLY_ATTRIBUTES = { "KERBEROS_PRINCIPAL", "LDAP_ID", "LDAP_ENTRY_DN", "CREATED_TIMESTAMP", "createTimestamp", "modifyTimestamp", "userCertificate", "saml.persistent.name.id.for.*", "ENABLED", "EMAIL_VERIFIED", "disabledReason" };
    private static String[] DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES = { "KERBEROS_PRINCIPAL", "LDAP_ID", "LDAP_ENTRY_DN", "CREATED_TIMESTAMP", "createTimestamp", "modifyTimestamp" };
    private static Pattern readOnlyAttributesPattern = getRegexPatternString(DEFAULT_READ_ONLY_ATTRIBUTES);
    private static Pattern adminReadOnlyAttributesPattern = getRegexPatternString(DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES);

    protected final Map<UserProfileContext, UserProfileMetadata> contextualMetadataRegistry;
    protected final KeycloakSession session;

    public AbstractUserProfileProvider() {
        // for reflection
        this(null, new HashMap<>());
    }

    public AbstractUserProfileProvider(KeycloakSession session, Map<UserProfileContext, UserProfileMetadata> contextualMetadataRegistry) {
        this.session = session;
        this.contextualMetadataRegistry = contextualMetadataRegistry;
    }

    @Override
    public UserProfile create(UserProfileContext context, UserModel user) {
        return createUserProfile(context, user.getAttributes(), user);
    }

    @Override
    public UserProfile create(UserProfileContext context, Map<String, ?> attributes, UserModel user) {
        return createUserProfile(context, attributes, user);
    }

    @Override
    public UserProfile create(UserProfileContext context, Map<String, ?> attributes) {
        return createUserProfile(context, attributes, null);
    }

    @Override
    public U create(KeycloakSession session) {
        return create(session, contextualMetadataRegistry);
    }

    @Override
    public void init(Config.Scope config) {
        Pattern pattern = getRegexPatternString(config.getArray("read-only-attributes"));
        AttributeValidatorMetadata readOnlyValidator = null;

        if (pattern != null) {
            readOnlyValidator = Validators.create(Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED, isReadOnlyAttributeUnchanged(pattern));
        }

        addContextualProfileMetadata(configureUserProfile(createBrokeringProfile(readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(ACCOUNT, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(ACCOUNT_OLD, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(REGISTRATION_PROFILE, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(UPDATE_PROFILE, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createRegistrationUserCreationProfile()));
        addContextualProfileMetadata(configureUserProfile(createUserResourceValidation(config)));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }

    @Override
    public String getConfiguration() {
        return null;
    }

    @Override
    public void setConfiguration(String configuration) {

    }

    /**
     * Subclasses can override this method to create their instances of {@link UserProfileProvider}.
     *
     * @param session the session
     * @param metadataRegistry the profile metadata
     *
     * @return the profile provider instance
     */
    protected abstract U create(KeycloakSession session, Map<UserProfileContext, UserProfileMetadata> metadataRegistry);

    /**
     * Sub-types can override this method to customize how contextual profile metadata is configured at init time.
     *
     * @param metadata the profile metadata
     * @return the metadata
     */
    protected UserProfileMetadata configureUserProfile(UserProfileMetadata metadata) {
        return metadata;
    }

    /**
     * Sub-types can override this method to customize how contextual profile metadata is configured at runtime.
     *
     * @param metadata the profile metadata
     * @param metadata the current session
     * @return the metadata
     */
    protected UserProfileMetadata configureUserProfile(UserProfileMetadata metadata, KeycloakSession session) {
        return metadata;
    }

    /**
     * Creates a {@link Function} for creating new users when the creating them using {@link UserProfile#create()}.
     *
     * @return a function for creating new users.
     */
    private Function<Attributes, UserModel> createUserFactory() {
        return new Function<Attributes, UserModel>() {
            private UserModel user;

            @Override
            public UserModel apply(Attributes attributes) {
                if (user == null) {
                    String userName = attributes.getFirstValue(UserModel.USERNAME);

                    // fallback to email in case email is allowed
                    if (userName == null) {
                        userName = attributes.getFirstValue(UserModel.EMAIL);
                    }

                    user = session.users().addUser(session.getContext().getRealm(), userName);
                }

                return user;
            }
        };
    }

    private UserProfile createUserProfile(UserProfileContext context, Map<String, ?> attributes, UserModel user) {
        UserProfileMetadata metadata = configureUserProfile(contextualMetadataRegistry.get(context), session);
        Attributes profileAttributes = new DefaultAttributes(context, attributes, user, metadata, session);
        return new DefaultUserProfile(profileAttributes, createUserFactory(), user);
    }

    private void addContextualProfileMetadata(UserProfileMetadata metadata) {
        if (contextualMetadataRegistry.putIfAbsent(metadata.getContext(), metadata) != null) {
            throw new IllegalStateException("Multiple profile metadata found for context " + metadata.getContext());
        }
    }

    private UserProfileMetadata createRegistrationUserCreationProfile() {
        UserProfileMetadata metadata = new UserProfileMetadata(REGISTRATION_USER_CREATION);

        metadata.addAttribute(UserModel.USERNAME, Validators.create(Messages.MISSING_USERNAME, (context) -> {
            RealmModel realm = context.getSession().getContext().getRealm();

            if (!realm.isRegistrationEmailAsUsername()) {
                return true;
            }

            return Validators.isBlank().validate(context);
        }), Validators.create(Messages.USERNAME_EXISTS,
                (context) -> {
                    KeycloakSession session = context.getSession();
                    RealmModel realm = session.getContext().getRealm();

                    if (realm.isRegistrationEmailAsUsername()) {
                        return true;
                    }

                    Map.Entry<String, List<String>> attribute = context.getAttribute();
                    List<String> values = attribute.getValue();

                    if (values.isEmpty()) {
                        return true;
                    }

                    String value = values.get(0);

                    UserModel existing = session.users().getUserByUsername(realm, value);
                    return existing == null;
                }));

        metadata.addAttribute(UserModel.EMAIL, Validators.create(Messages.INVALID_EMAIL, (context) -> {
            RealmModel realm = context.getSession().getContext().getRealm();

            if (!realm.isRegistrationEmailAsUsername()) {
                return true;
            }

            Map.Entry<String, List<String>> attribute = context.getAttribute();
            List<String> values = attribute.getValue();

            if (values.isEmpty()) {
                return true;
            }

            String value = values.get(0);

            return Validation.isBlank(value) || Validation.isEmailValid(value);
        }));

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, new AttributeValidatorMetadata(UPDATE_READ_ONLY_ATTRIBUTES_REJECTED, isReadOnlyAttributeUnchanged(readOnlyAttributesPattern)));

        return metadata;
    }

    private UserProfileMetadata createDefaultProfile(UserProfileContext context, AttributeValidatorMetadata readOnlyValidator) {
        UserProfileMetadata metadata = new UserProfileMetadata(context);

        metadata.addAttribute(UserModel.USERNAME, Validators.create(Messages.MISSING_USERNAME, Validators.checkUsernameExists()),
                Validators.create(Messages.USERNAME_EXISTS, Validators.userNameExists()),
                Validators.create(Messages.READ_ONLY_USERNAME, Validators.isUserMutable()));

        metadata.addAttribute(UserModel.FIRST_NAME, Validators.create(Messages.MISSING_FIRST_NAME, Validators.isBlank()));

        metadata.addAttribute(UserModel.LAST_NAME, Validators.create(Messages.MISSING_LAST_NAME, Validators.isBlank()));

        metadata.addAttribute(UserModel.EMAIL, Validators.create(Messages.MISSING_EMAIL, Validators.isBlank()),
                Validators.create(Messages.INVALID_EMAIL, Validators.isEmailValid()),
                Validators.create(Messages.EMAIL_EXISTS, Validators.isEmailDuplicated()),
                Validators.create(Messages.USERNAME_EXISTS, Validators.doesEmailExistAsUsername()));

        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        readonlyValidators.add(new AttributeValidatorMetadata(UPDATE_READ_ONLY_ATTRIBUTES_REJECTED,
                isReadOnlyAttributeUnchanged(readOnlyAttributesPattern)));

        if (readOnlyValidator != null) {
            readonlyValidators.add(readOnlyValidator);
        }

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, readonlyValidators);

        return metadata;
    }

    private UserProfileMetadata createBrokeringProfile(AttributeValidatorMetadata readOnlyValidator) {
        UserProfileMetadata metadata = new UserProfileMetadata(IDP_REVIEW);

        metadata.addAttribute(UserModel.USERNAME, Validators
                        .create(Messages.MISSING_USERNAME, Validators.checkFederatedUsernameExists()));

        metadata.addAttribute(UserModel.FIRST_NAME,
                Validators.create(Messages.MISSING_FIRST_NAME, Validators.isBlank()));

        metadata.addAttribute(UserModel.LAST_NAME,
                Validators.create(Messages.MISSING_LAST_NAME, Validators.isBlank()));

        metadata.addAttribute(UserModel.EMAIL, Validators.create(Messages.MISSING_EMAIL, Validators.isBlank()),
                Validators.create(Messages.INVALID_EMAIL, Validators.isEmailValid()));

        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        readonlyValidators.add(new AttributeValidatorMetadata(UPDATE_READ_ONLY_ATTRIBUTES_REJECTED,
                isReadOnlyAttributeUnchanged(readOnlyAttributesPattern)));

        if (readOnlyValidator != null) {
            readonlyValidators.add(readOnlyValidator);
        }

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, readonlyValidators);

        return metadata;
    }

    private UserProfileMetadata createUserResourceValidation(Config.Scope config) {
        Pattern p = getRegexPatternString(config.getArray("admin-read-only-attributes"));
        UserProfileMetadata metadata = new UserProfileMetadata(USER_API);
        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        if (p != null) {
            readonlyValidators.add(Validators.create(Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED, isReadOnlyAttributeUnchanged(p)));
        }

        readonlyValidators.add(new AttributeValidatorMetadata(UPDATE_READ_ONLY_ATTRIBUTES_REJECTED,
                isReadOnlyAttributeUnchanged(adminReadOnlyAttributesPattern)));

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, readonlyValidators);

        return metadata;
    }
}