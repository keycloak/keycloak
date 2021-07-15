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

import static org.keycloak.userprofile.DefaultAttributes.READ_ONLY_ATTRIBUTE_KEY;
import static org.keycloak.userprofile.UserProfileContext.ACCOUNT;
import static org.keycloak.userprofile.UserProfileContext.ACCOUNT_OLD;
import static org.keycloak.userprofile.UserProfileContext.IDP_REVIEW;
import static org.keycloak.userprofile.UserProfileContext.REGISTRATION_PROFILE;
import static org.keycloak.userprofile.UserProfileContext.REGISTRATION_USER_CREATION;
import static org.keycloak.userprofile.UserProfileContext.UPDATE_PROFILE;
import static org.keycloak.userprofile.UserProfileContext.USER_API;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.userprofile.validator.BlankAttributeValidator;
import org.keycloak.userprofile.validator.BrokeringFederatedUsernameHasValueValidator;
import org.keycloak.userprofile.validator.DuplicateEmailValidator;
import org.keycloak.userprofile.validator.DuplicateUsernameValidator;
import org.keycloak.userprofile.validator.EmailExistsAsUsernameValidator;
import org.keycloak.userprofile.validator.ReadOnlyAttributeUnchangedValidator;
import org.keycloak.userprofile.validator.RegistrationEmailAsUsernameEmailValueValidator;
import org.keycloak.userprofile.validator.RegistrationEmailAsUsernameUsernameValueValidator;
import org.keycloak.userprofile.validator.RegistrationUsernameExistsValidator;
import org.keycloak.userprofile.validator.UsernameHasValueValidator;
import org.keycloak.userprofile.validator.UsernameMutationValidator;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.validators.EmailValidator;

/**
 * <p>A base class for {@link UserProfileProvider} implementations providing the main hooks for customizations.
 *
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public abstract class AbstractUserProfileProvider<U extends UserProfileProvider> implements UserProfileProvider, UserProfileProviderFactory<U> {

    private static boolean editUsernameCondition(AttributeContext c) {
        KeycloakSession session = c.getSession();
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        switch (c.getContext()) {
            case REGISTRATION_PROFILE:
            case IDP_REVIEW:
                return !realm.isRegistrationEmailAsUsername();
            case ACCOUNT_OLD:
            case ACCOUNT:
            case UPDATE_PROFILE:
                return realm.isEditUsernameAllowed();
            case USER_API:
                return true;
            default:
                return false;
        }
    }

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

    /**
     * There are the declarations for creating the built-in validations for read-only attributes. Regardless of the context where
     * user profiles are used. They are related to internal attributes with hard conditions on them in terms of management.
     */
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
        // make sure registry is clear in case of re-deploy
        contextualMetadataRegistry.clear();
        Pattern pattern = getRegexPatternString(config.getArray("read-only-attributes"));
        AttributeValidatorMetadata readOnlyValidator = null;

        if (pattern != null) {
            readOnlyValidator = createReadOnlyAttributeUnchangedValidator(pattern);
        }

        addContextualProfileMetadata(configureUserProfile(createBrokeringProfile(readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(ACCOUNT, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(ACCOUNT_OLD, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(REGISTRATION_PROFILE, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(UPDATE_PROFILE, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createRegistrationUserCreationProfile()));
        addContextualProfileMetadata(configureUserProfile(createUserResourceValidation(config)));
    }
    
    private AttributeValidatorMetadata createReadOnlyAttributeUnchangedValidator(Pattern pattern) {
        return new AttributeValidatorMetadata(ReadOnlyAttributeUnchangedValidator.ID,
                ValidatorConfig.builder().config(ReadOnlyAttributeUnchangedValidator.CFG_PATTERN, pattern)
                        .build());
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
        Attributes profileAttributes = createAttributes(context, attributes, user, metadata);
        return new DefaultUserProfile(metadata, profileAttributes, createUserFactory(), user, session);
    }

    protected Attributes createAttributes(UserProfileContext context, Map<String, ?> attributes, UserModel user,
            UserProfileMetadata metadata) {
        return new DefaultAttributes(context, attributes, user, metadata, session);
    }

    private void addContextualProfileMetadata(UserProfileMetadata metadata) {
        if (contextualMetadataRegistry.putIfAbsent(metadata.getContext(), metadata) != null) {
            throw new IllegalStateException("Multiple profile metadata found for context " + metadata.getContext());
        }
    }

    private UserProfileMetadata createRegistrationUserCreationProfile() {
        UserProfileMetadata metadata = new UserProfileMetadata(REGISTRATION_USER_CREATION);

        metadata.addAttribute(UserModel.USERNAME, -2, new AttributeValidatorMetadata(RegistrationEmailAsUsernameUsernameValueValidator.ID), new AttributeValidatorMetadata(RegistrationUsernameExistsValidator.ID));

        metadata.addAttribute(UserModel.EMAIL, -1, new AttributeValidatorMetadata(RegistrationEmailAsUsernameEmailValueValidator.ID));

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, 1000, createReadOnlyAttributeUnchangedValidator(readOnlyAttributesPattern));

        return metadata;
    }

    private UserProfileMetadata createDefaultProfile(UserProfileContext context, AttributeValidatorMetadata readOnlyValidator) {
        UserProfileMetadata metadata = new UserProfileMetadata(context);

        metadata.addAttribute(UserModel.USERNAME, -2, AbstractUserProfileProvider::editUsernameCondition,
                AbstractUserProfileProvider::editUsernameCondition,
                new AttributeValidatorMetadata(UsernameHasValueValidator.ID),
                new AttributeValidatorMetadata(DuplicateUsernameValidator.ID),
                new AttributeValidatorMetadata(UsernameMutationValidator.ID)).setAttributeDisplayName("${username}");

        metadata.addAttribute(UserModel.EMAIL, -1, new AttributeValidatorMetadata(BlankAttributeValidator.ID, BlankAttributeValidator.createConfig(Messages.MISSING_EMAIL, false)),
        		new AttributeValidatorMetadata(EmailValidator.ID, ValidatorConfig.builder().config(EmailValidator.IGNORE_EMPTY_VALUE, true).build()),
        		new AttributeValidatorMetadata(DuplicateEmailValidator.ID),
        		new AttributeValidatorMetadata(EmailExistsAsUsernameValidator.ID)).setAttributeDisplayName("${email}");

        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        readonlyValidators.add(createReadOnlyAttributeUnchangedValidator(readOnlyAttributesPattern));

        if (readOnlyValidator != null) {
            readonlyValidators.add(readOnlyValidator);
        }

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, 1000, readonlyValidators);

        return metadata;
    }

    private UserProfileMetadata createBrokeringProfile(AttributeValidatorMetadata readOnlyValidator) {
        UserProfileMetadata metadata = new UserProfileMetadata(IDP_REVIEW);

        metadata.addAttribute(UserModel.USERNAME, -2, AbstractUserProfileProvider::editUsernameCondition,
                AbstractUserProfileProvider::editUsernameCondition, new AttributeValidatorMetadata(BrokeringFederatedUsernameHasValueValidator.ID)).setAttributeDisplayName("${username}");

        metadata.addAttribute(UserModel.EMAIL, -1, new AttributeValidatorMetadata(BlankAttributeValidator.ID, BlankAttributeValidator.createConfig(Messages.MISSING_EMAIL, true)),
        		new AttributeValidatorMetadata(EmailValidator.ID)).setAttributeDisplayName("${email}");

        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        readonlyValidators.add(createReadOnlyAttributeUnchangedValidator(readOnlyAttributesPattern));

        if (readOnlyValidator != null) {
            readonlyValidators.add(readOnlyValidator);
        }

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, 1000, readonlyValidators);

        return metadata;
    }

    private UserProfileMetadata createUserResourceValidation(Config.Scope config) {
        Pattern p = getRegexPatternString(config.getArray("admin-read-only-attributes"));
        UserProfileMetadata metadata = new UserProfileMetadata(USER_API);
        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        if (p != null) {
            readonlyValidators.add(createReadOnlyAttributeUnchangedValidator(p));
        }

        readonlyValidators.add(createReadOnlyAttributeUnchangedValidator(adminReadOnlyAttributesPattern));

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, 1000, readonlyValidators);

        return metadata;
    }
}