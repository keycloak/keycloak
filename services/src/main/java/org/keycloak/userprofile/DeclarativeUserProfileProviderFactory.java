/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.userprofile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.authentication.requiredactions.UpdateEmail;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.validator.OrganizationMemberValidator;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.messages.Messages;
import org.keycloak.userprofile.config.UPConfigUtils;
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
import org.keycloak.utils.StringUtil;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.validators.EmailValidator;

import static java.util.Optional.ofNullable;

import static org.keycloak.common.util.ObjectUtil.isBlank;
import static org.keycloak.userprofile.DefaultAttributes.READ_ONLY_ATTRIBUTE_KEY;
import static org.keycloak.userprofile.UserProfileContext.ACCOUNT;
import static org.keycloak.userprofile.UserProfileContext.IDP_REVIEW;
import static org.keycloak.userprofile.UserProfileContext.REGISTRATION;
import static org.keycloak.userprofile.UserProfileContext.UPDATE_EMAIL;
import static org.keycloak.userprofile.UserProfileContext.UPDATE_PROFILE;
import static org.keycloak.userprofile.UserProfileContext.USER_API;

public class DeclarativeUserProfileProviderFactory implements UserProfileProviderFactory, AmphibianProviderFactory<UserProfileProvider> {

    public static final String CONFIG_ADMIN_READ_ONLY_ATTRIBUTES = "admin-read-only-attributes";
    public static final String CONFIG_READ_ONLY_ATTRIBUTES = "read-only-attributes";
    public static final String MAX_EMAIL_LOCAL_PART_LENGTH = "max-email-local-part-length";

    public static final String ID = "declarative-user-profile";
    public static final int PROVIDER_PRIORITY = 1;

    /**
     * There are the declarations for creating the built-in validations for read-only attributes. Regardless of the context where
     * user profiles are used. They are related to internal attributes with hard conditions on them in terms of management.
     */
    private static final String[] DEFAULT_READ_ONLY_ATTRIBUTES = { "KERBEROS_PRINCIPAL", "LDAP_ID", "LDAP_ENTRY_DN", "CREATED_TIMESTAMP", "createTimestamp", "modifyTimestamp", "userCertificate", "saml.persistent.name.id.for.*", "ENABLED", "EMAIL_VERIFIED", "disabledReason", UserModel.EMAIL_PENDING };
    private static final String[] DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES = { "KERBEROS_PRINCIPAL", "LDAP_ID", "LDAP_ENTRY_DN", "CREATED_TIMESTAMP", "createTimestamp", "modifyTimestamp" };
    private static final Pattern readOnlyAttributesPattern = getRegexPatternString(DEFAULT_READ_ONLY_ATTRIBUTES);
    private static final Pattern adminReadOnlyAttributesPattern = getRegexPatternString(DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES);

    private static volatile UPConfig PARSED_DEFAULT_RAW_CONFIG;
    private final Map<UserProfileContext, UserProfileMetadata> contextualMetadataRegistry = new HashMap<>();

    public static void setDefaultConfig(UPConfig defaultConfig) {
        if (PARSED_DEFAULT_RAW_CONFIG == null) {
            PARSED_DEFAULT_RAW_CONFIG = defaultConfig;
        }
    }

    private static boolean editUsernameCondition(AttributeContext c) {
        KeycloakSession session = c.getSession();
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        if (REGISTRATION.equals(c.getContext())
                || IDP_REVIEW.equals(c.getContext())
                || isNewUser(c)) {
            return !realm.isRegistrationEmailAsUsername();
        }

        if (realm.isRegistrationEmailAsUsername()) {
            return false;
        }

        return realm.isEditUsernameAllowed();
    }

    private static boolean readUsernameCondition(AttributeContext c) {
        KeycloakSession session = c.getSession();
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        switch (c.getContext()) {
            case REGISTRATION:
            case IDP_REVIEW:
                return !realm.isRegistrationEmailAsUsername();
            case UPDATE_PROFILE:
                if (realm.isRegistrationEmailAsUsername()) {
                    return false;
                }
                return realm.isEditUsernameAllowed();
            case UPDATE_EMAIL:
                return false;
        }

        return true;
    }

    private static boolean editEmailCondition(AttributeContext c) {
        RealmModel realm = c.getSession().getContext().getRealm();

        if (REGISTRATION.equals(c.getContext()) || USER_API.equals(c.getContext())) {
            return true;
        }

        if (UpdateEmail.isEnabled(realm)) {
            if (UPDATE_PROFILE.equals(c.getContext())) {
                UserModel user = c.getUser();

                if (!isNewUser(c)) {
                    if (StringUtil.isBlank(user.getEmail())) {
                        // allow to set email via UPDATE_PROFILE if the email is not set for the user
                        return true;
                    }

                    List<String> values = c.getAttribute().getValue();

                    if (values == null || values.isEmpty()) {
                        // ignore empty values if the user has an email set, email should be set via update email flow
                        return false;
                    }
                }
            }

            return !(UPDATE_PROFILE.equals(c.getContext()) || ACCOUNT.equals(c.getContext()));
        }

        if (!isNewUser(c) && realm.isRegistrationEmailAsUsername() && !realm.isEditUsernameAllowed()) {
            return false;
        }

        return true;
    }

    private static boolean readEmailCondition(AttributeContext c) {
        UserProfileContext context = c.getContext();

        if (REGISTRATION.equals(context) || USER_API.equals(c.getContext())) {
            return true;
        }

        KeycloakSession session = c.getSession();

        if (UpdateEmail.isEnabled(session.getContext().getRealm())) {
            if (UPDATE_PROFILE.equals(c.getContext())) {
                List<String> value = c.getAttribute().getValue();

                if (value.isEmpty() && !c.getMetadata().isReadOnly(c)) {
                    // show email field in UPDATE_PROFILE page if the email is not set for the user and is not read-only
                    return true;
                }
            }

            return !UPDATE_PROFILE.equals(context);
        }

        if (UPDATE_PROFILE.equals(context)) {
            RealmModel realm = session.getContext().getRealm();

            if (realm.isRegistrationEmailAsUsername()) {
                return realm.isEditUsernameAllowed();
            }
        }

        return true;
    }

    private static boolean isInternationalizationEnabled(AttributeContext context) {
        RealmModel realm = context.getSession().getContext().getRealm();
        return realm.isInternationalizationEnabled();
    }

    private static boolean isTermAndConditionsEnabled(AttributeContext context) {
        RealmModel realm = context.getSession().getContext().getRealm();
        RequiredActionProviderModel tacModel = realm.getRequiredActionProviderByAlias(
                UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
        return tacModel != null && tacModel.isEnabled();
    }

    private static boolean isNewUser(AttributeContext c) {
        return c.getUser() == null;
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

    @Override
    public void init(Config.Scope config) {
        initDefaultConfiguration(config);

        // make sure registry is clear in case of re-deploy
        contextualMetadataRegistry.clear();
        Pattern pattern = getRegexPatternString(config.getArray(CONFIG_READ_ONLY_ATTRIBUTES));
        AttributeValidatorMetadata readOnlyValidator = null;

        if (pattern != null) {
            readOnlyValidator = createReadOnlyAttributeUnchangedValidator(pattern);
        }

        addContextualProfileMetadata(configureUserProfile(createBrokeringProfile(readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createAccountProfile(ACCOUNT, readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createDefaultProfile(UPDATE_PROFILE, readOnlyValidator)));
        if (Profile.isFeatureEnabled(Profile.Feature.UPDATE_EMAIL)) {
            addContextualProfileMetadata(configureUserProfile(createDefaultProfile(UPDATE_EMAIL, readOnlyValidator)));
        }
        addContextualProfileMetadata(configureUserProfile(createRegistrationUserCreationProfile(readOnlyValidator)));
        addContextualProfileMetadata(configureUserProfile(createUserResourceValidation(config)));
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CONFIG_READ_ONLY_ATTRIBUTES)
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .helpText("Array of regular expressions to identify fields that should be treated read-only so users can't change them.")
                .add()

                .property()
                .name(CONFIG_ADMIN_READ_ONLY_ATTRIBUTES)
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .helpText("Array of regular expressions to identify fields that should be treated read-only so administrators can't change them.")
                .add()

                .property()
                .name(MAX_EMAIL_LOCAL_PART_LENGTH)
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("To set user profile max email local part length")
                .add()

                .build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name(DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .build();
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        String upConfigJson = model == null ? null : model.get(DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY);

        if (!isBlank(upConfigJson)) {
            try {
                UPConfig upc = UPConfigUtils.parseConfig(upConfigJson);
                List<String> errors = UPConfigUtils.validate(session, upc);

                if (!errors.isEmpty()) {
                    throw new ComponentValidationException(errors.toString());
                }
            } catch (IOException e) {
                throw new ComponentValidationException(e.getMessage(), e);
            }
        }

        // delete cache so new config is parsed and applied next time it is required
        // throught #configureUserProfile(metadata, session)
        if (model != null) {
            model.removeNote(DeclarativeUserProfileProvider.PARSED_CONFIG_COMPONENT_KEY);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public DeclarativeUserProfileProvider create(KeycloakSession session) {
        return new DeclarativeUserProfileProvider(session, this);
    }

    /**
     * Specifies how contextual profile metadata is configured at init time.
     *
     * @param metadata the profile metadata
     * @return the metadata
     */
    protected UserProfileMetadata configureUserProfile(UserProfileMetadata metadata) {
        // default metadata for each context is based on the default realm configuration
        return new DeclarativeUserProfileProvider(null, this).decorateUserProfileForCache(metadata, PARSED_DEFAULT_RAW_CONFIG);
    }

    private AttributeValidatorMetadata createReadOnlyAttributeUnchangedValidator(Pattern pattern) {
        return new AttributeValidatorMetadata(ReadOnlyAttributeUnchangedValidator.ID,
                ValidatorConfig.builder().config(ReadOnlyAttributeUnchangedValidator.CFG_PATTERN, pattern)
                        .build());
    }

    private void addContextualProfileMetadata(UserProfileMetadata metadata) {
        if (contextualMetadataRegistry.putIfAbsent(metadata.getContext(), metadata) != null) {
            throw new IllegalStateException("Multiple profile metadata found for context " + metadata.getContext());
        }

        if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            for (AttributeMetadata attribute : metadata.getAttributes()) {
                String name = attribute.getName();

                if (UserModel.EMAIL.equals(name)) {
                    attribute.addValidators(List.of(new AttributeValidatorMetadata(OrganizationMemberValidator.ID)));
                }
            }
        }
    }

    private UserProfileMetadata createBrokeringProfile(AttributeValidatorMetadata readOnlyValidator) {
        UserProfileMetadata metadata = new UserProfileMetadata(IDP_REVIEW);

        metadata.addAttribute(UserModel.USERNAME, -2, DeclarativeUserProfileProviderFactory::editUsernameCondition,
                DeclarativeUserProfileProviderFactory::readUsernameCondition, new AttributeValidatorMetadata(BrokeringFederatedUsernameHasValueValidator.ID)).setAttributeDisplayName("${username}");

        metadata.addAttribute(UserModel.EMAIL, -1,
                        new AttributeValidatorMetadata(BlankAttributeValidator.ID, BlankAttributeValidator.createConfig(Messages.MISSING_EMAIL, true)))
                .setAttributeDisplayName("${email}");

        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        readonlyValidators.add(createReadOnlyAttributeUnchangedValidator(readOnlyAttributesPattern));

        if (readOnlyValidator != null) {
            readonlyValidators.add(readOnlyValidator);
        }

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, 1000, readonlyValidators);

        return metadata;
    }

    private UserProfileMetadata createRegistrationUserCreationProfile(AttributeValidatorMetadata readOnlyValidator) {
        UserProfileMetadata metadata = createDefaultProfile(REGISTRATION, readOnlyValidator);

        metadata.getAttribute(UserModel.USERNAME).get(0).addValidators(Arrays.asList(
                new AttributeValidatorMetadata(RegistrationEmailAsUsernameUsernameValueValidator.ID), new AttributeValidatorMetadata(RegistrationUsernameExistsValidator.ID), new AttributeValidatorMetadata(UsernameHasValueValidator.ID)));

        metadata.getAttribute(UserModel.EMAIL).get(0).addValidators(Collections.singletonList(
                new AttributeValidatorMetadata(RegistrationEmailAsUsernameEmailValueValidator.ID)));

        metadata.addAttribute(UserModel.LOCALE, -1, DeclarativeUserProfileProviderFactory::isInternationalizationEnabled, DeclarativeUserProfileProviderFactory::isInternationalizationEnabled)
                .setRequired(AttributeMetadata.ALWAYS_FALSE);

        return metadata;
    }

    private UserProfileMetadata createDefaultProfile(UserProfileContext context, AttributeValidatorMetadata readOnlyValidator) {
        UserProfileMetadata metadata = new UserProfileMetadata(context);

        metadata.addAttribute(UserModel.USERNAME, -2,
                DeclarativeUserProfileProviderFactory::editUsernameCondition,
                DeclarativeUserProfileProviderFactory::readUsernameCondition,
                new AttributeValidatorMetadata(UsernameHasValueValidator.ID),
                new AttributeValidatorMetadata(DuplicateUsernameValidator.ID),
                new AttributeValidatorMetadata(UsernameMutationValidator.ID)).setAttributeDisplayName("${username}");

        metadata.addAttribute(UserModel.EMAIL, -1,
                        DeclarativeUserProfileProviderFactory::editEmailCondition,
                        DeclarativeUserProfileProviderFactory::readEmailCondition,
                        new AttributeValidatorMetadata(BlankAttributeValidator.ID, BlankAttributeValidator.createConfig(Messages.MISSING_EMAIL, false)),
                        new AttributeValidatorMetadata(DuplicateEmailValidator.ID),
                        new AttributeValidatorMetadata(EmailExistsAsUsernameValidator.ID),
                        new AttributeValidatorMetadata(EmailValidator.ID, ValidatorConfig.builder().config(EmailValidator.IGNORE_EMPTY_VALUE, true).build()))
                .setAttributeDisplayName("${email}")
                .setAnnotationDecorator(DeclarativeUserProfileProviderFactory::getEmailAnnotationDecorator);

        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        readonlyValidators.add(createReadOnlyAttributeUnchangedValidator(readOnlyAttributesPattern));

        if (readOnlyValidator != null) {
            readonlyValidators.add(readOnlyValidator);
        }

        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, 1000, readonlyValidators);

        return metadata;
    }

    private UserProfileMetadata createUserResourceValidation(Config.Scope config) {
        Pattern p = getRegexPatternString(config.getArray(CONFIG_ADMIN_READ_ONLY_ATTRIBUTES));
        UserProfileMetadata metadata = new UserProfileMetadata(USER_API);


        metadata.addAttribute(UserModel.USERNAME, -2,
                        new AttributeValidatorMetadata(UsernameHasValueValidator.ID),
                        new AttributeValidatorMetadata(DuplicateUsernameValidator.ID))
                .addWriteCondition(DeclarativeUserProfileProviderFactory::editUsernameCondition);
        metadata.addAttribute(UserModel.EMAIL, -1,
                        new AttributeValidatorMetadata(DuplicateEmailValidator.ID),
                        new AttributeValidatorMetadata(EmailExistsAsUsernameValidator.ID),
                        new AttributeValidatorMetadata(EmailValidator.ID, ValidatorConfig.builder().config(EmailValidator.IGNORE_EMPTY_VALUE, true).build()))
                .addWriteCondition(DeclarativeUserProfileProviderFactory::editEmailCondition);

        List<AttributeValidatorMetadata> readonlyValidators = new ArrayList<>();

        if (p != null) {
            readonlyValidators.add(createReadOnlyAttributeUnchangedValidator(p));
        }

        readonlyValidators.add(createReadOnlyAttributeUnchangedValidator(adminReadOnlyAttributesPattern));
        metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, 1000, readonlyValidators);

        metadata.addAttribute(UserModel.LOCALE, -1, DeclarativeUserProfileProviderFactory::isInternationalizationEnabled, DeclarativeUserProfileProviderFactory::isInternationalizationEnabled)
                .setRequired(AttributeMetadata.ALWAYS_FALSE);
        metadata.addAttribute(UserModel.EMAIL_PENDING, -1, this::isUpdateEmailFeatureEnabled, this::isUpdateEmailFeatureEnabled)
                .setAttributeDisplayName("${emailPendingVerification}")
                .setRequired(AttributeMetadata.ALWAYS_FALSE);

        metadata.addAttribute(TermsAndConditions.USER_ATTRIBUTE, -1, AttributeMetadata.ALWAYS_FALSE,
                DeclarativeUserProfileProviderFactory::isTermAndConditionsEnabled)
                .setAttributeDisplayName("${termsAndConditionsUserAttribute}")
                .setRequired(AttributeMetadata.ALWAYS_FALSE);

        return metadata;
    }

    private UserProfileMetadata createAccountProfile(UserProfileContext context, AttributeValidatorMetadata readOnlyValidator) {
        UserProfileMetadata defaultProfile = createDefaultProfile(context, readOnlyValidator);

        defaultProfile.addAttribute(UserModel.LOCALE, -1, DeclarativeUserProfileProviderFactory::isInternationalizationEnabled, DeclarativeUserProfileProviderFactory::isInternationalizationEnabled)
                .setRequired(AttributeMetadata.ALWAYS_FALSE);

        return defaultProfile;
    }

    // GETTER METHODS FOR INTERNAL FIELDS

    protected UPConfig getParsedDefaultRawConfig() {
        return PARSED_DEFAULT_RAW_CONFIG;
    }

    protected Map<UserProfileContext, UserProfileMetadata> getContextualMetadataRegistry() {
        return contextualMetadataRegistry;
    }

    private void initDefaultConfiguration(Scope config) {
        // The user-defined configuration is always parsed during init and should be avoided as much as possible
        // If no user-defined configuration is set, the system default configuration must have been set
        // In Quarkus, the system default configuration is set at build time for optimization purposes
        UPConfig defaultConfig = ofNullable(config.get("configFile"))
                .map(Paths::get)
                .map(UPConfigUtils::parseConfig)
                .orElse(PARSED_DEFAULT_RAW_CONFIG);

        if (defaultConfig == null) {
            // as a fallback parse the system default config
            defaultConfig = UPConfigUtils.parseSystemDefaultConfig();
        }

        PARSED_DEFAULT_RAW_CONFIG = null;
        setDefaultConfig(defaultConfig);
    }

    private static Map<String, Object> getEmailAnnotationDecorator(AttributeContext c) {
        AttributeMetadata m = c.getMetadata();
        Map<String, Object> rawAnnotations = Optional.ofNullable(m.getAnnotations()).orElse(Map.of());

        KeycloakSession session = c.getSession();
        KeycloakContext context = session.getContext();

        if (UpdateEmail.isEnabled(context.getRealm())) {
            UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
            UPConfig upConfig = provider.getConfiguration();
            UPAttribute attribute = upConfig.getAttribute(UserModel.EMAIL);
            UPAttributePermissions permissions = attribute.getPermissions();

            if (permissions == null) {
                return rawAnnotations;
            }

            Set<String> writePermissions = permissions.getEdit();
            boolean isWritable = writePermissions.contains(UPConfigUtils.ROLE_USER);
            RealmModel realm = context.getRealm();

            if ((realm.isRegistrationEmailAsUsername() && !realm.isEditUsernameAllowed()) || !isWritable) {
                return rawAnnotations;
            }

            Map<String, Object> annotations = new HashMap<>(rawAnnotations);

            annotations.put("kc.required.action.supported", isWritable);

            return annotations;
        }

        return rawAnnotations;
    }

    private boolean isUpdateEmailFeatureEnabled(AttributeContext context) {
        Entry<String, List<String>> attribute = context.getAttribute();

        if (attribute.getValue().isEmpty()) {
            return false;
        }

        KeycloakSession session = context.getSession();
        KeycloakContext context1 = session.getContext();
        RealmModel realm = context1.getRealm();

        return UpdateEmail.isEnabled(realm);
    }
}
