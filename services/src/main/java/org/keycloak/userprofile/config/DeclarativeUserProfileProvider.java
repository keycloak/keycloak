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

package org.keycloak.userprofile.config;

import static org.keycloak.common.util.ObjectUtil.isBlank;
import static org.keycloak.protocol.oidc.TokenManager.getRequestedClientScopes;
import static org.keycloak.userprofile.config.UPConfigUtils.readConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.AttributeValidatorMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileMetadata;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.legacy.AbstractUserProfileProvider;
import org.keycloak.userprofile.validator.AttributeRequiredByMetadataValidator;
import org.keycloak.userprofile.validator.ImmutableAttributeValidator;
import org.keycloak.validate.AbstractSimpleValidator;
import org.keycloak.validate.ValidatorConfig;

/**
 * {@link UserProfileProvider} loading configuration from the changeable JSON file stored in component config. Parsed
 * configuration is cached.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author Vlastimil Elias <velias@redhat.com>
 */
public class DeclarativeUserProfileProvider extends AbstractUserProfileProvider<DeclarativeUserProfileProvider>
        implements AmphibianProviderFactory<DeclarativeUserProfileProvider>, EnvironmentDependentProviderFactory {

    public static final String SYSTEM_DEFAULT_CONFIG_RESOURCE = "keycloak-default-user-profile.json";
    public static final String ID = "declarative-user-profile";
    public static final String UP_PIECES_COUNT_COMPONENT_CONFIG_KEY = "config-pieces-count";
    private static final String PARSED_CONFIG_COMPONENT_KEY = "kc.user.profile.metadata";
    private static final String UP_PIECE_COMPONENT_CONFIG_KEY_BASE = "config-piece-";

    private static boolean createRequiredForScopePredicate(AttributeContext context, List<String> requiredScopes) {
        KeycloakSession session = context.getSession();
        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();

        if (authenticationSession == null) {
            return false;
        }

        String requestedScopesString = authenticationSession.getClientNote(OIDCLoginProtocol.SCOPE_PARAM);
        ClientModel client = authenticationSession.getClient();

        return getRequestedClientScopes(requestedScopesString, client).map((csm) -> csm.getName()).anyMatch(requiredScopes::contains);
    }

    private String defaultRawConfig;

    public DeclarativeUserProfileProvider() {
        // for reflection
    }

    public DeclarativeUserProfileProvider(KeycloakSession session, Map<UserProfileContext, UserProfileMetadata> metadataRegistry, String defaultRawConfig) {
        super(session, metadataRegistry);
        this.defaultRawConfig = defaultRawConfig;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected DeclarativeUserProfileProvider create(KeycloakSession session, Map<UserProfileContext, UserProfileMetadata> metadataRegistry) {
        return new DeclarativeUserProfileProvider(session, metadataRegistry, defaultRawConfig);
    }

    @Override
    protected Attributes createAttributes(UserProfileContext context, Map<String, ?> attributes,
            UserModel user, UserProfileMetadata metadata) {
        return new DeclarativeAttributes(context, attributes, user, metadata, session);
    }

    @Override
    protected UserProfileMetadata configureUserProfile(UserProfileMetadata metadata, KeycloakSession session) {
        ComponentModel model = getComponentModelOrCreate(session);
        Map<UserProfileContext, UserProfileMetadata> metadataMap = model.getNote(PARSED_CONFIG_COMPONENT_KEY);

        // not cached, create a note with cache
        if (metadataMap == null) {
            metadataMap = new HashMap<>();
            model.setNote(PARSED_CONFIG_COMPONENT_KEY, metadataMap);
        }

        return metadataMap.computeIfAbsent(metadata.getContext(), (context) -> decorateUserProfileForCache(metadata, model));
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        String upConfigJson = getConfigJsonFromComponentModel(model);

        if (!isBlank(upConfigJson)) {
            try {
                UPConfig upc = readConfig(new ByteArrayInputStream(upConfigJson.getBytes("UTF-8")));
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
            model.removeNote(PARSED_CONFIG_COMPONENT_KEY);
        }
    }

    @Override
    public String getConfiguration() {
        String cfg = getConfigJsonFromComponentModel(getComponentModel());

        if (isBlank(cfg)) {
            return defaultRawConfig;
        }

        return cfg;
    }

    @Override
    public void setConfiguration(String configuration) {
        ComponentModel component = getComponentModel();

        removeConfigJsonFromComponentModel(component);

        if (!isBlank(configuration)) {
            // store new parts
            List<String> parts = UPConfigUtils.getChunks(configuration, 3800);
            MultivaluedHashMap<String, String> config = component.getConfig();

            config.putSingle(UP_PIECES_COUNT_COMPONENT_CONFIG_KEY, "" + parts.size());

            int i = 0;

            for (String part : parts) {
                config.putSingle(UP_PIECE_COMPONENT_CONFIG_KEY_BASE + (i++), part);
            }
        }

        session.getContext().getRealm().updateComponent(component);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // TODO: We should avoid blocking operations during startup. Need to review this.
        try (InputStream is = getClass().getResourceAsStream(SYSTEM_DEFAULT_CONFIG_RESOURCE)) {
            defaultRawConfig = StreamUtil.readString(is, Charset.defaultCharset());
        } catch (IOException cause) {
            throw new RuntimeException("Failed to load default user profile config file", cause);
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    public ComponentModel getComponentModel() {
        return getComponentModelOrCreate(session);
    }

    /**
     * Decorate basic metadata provided from {@link AbstractUserProfileProvider} based on 'per realm' configuration.
     * This method is called for each {@link UserProfileContext} in each realm, and metadata are cached then and this
     * method is called again only if configuration changes.
     *
     * @param metadata base to be decorated based on configuration loaded from component model
     * @param model component model to get "per realm" configuration from
     * @return decorated metadata
     */
    protected UserProfileMetadata decorateUserProfileForCache(UserProfileMetadata metadata, ComponentModel model) {
        UserProfileContext context = metadata.getContext();
        UPConfig parsedConfig = getParsedConfig(model);

        // do not change config for REGISTRATION_USER_CREATION context, everything important is covered thanks to REGISTRATION_PROFILE
        if (parsedConfig == null || context == UserProfileContext.REGISTRATION_USER_CREATION) {
            return metadata;
        }

        // need to clone otherwise changes to profile config are going to be reflected
        // in the default config
        UserProfileMetadata decoratedMetadata = metadata.clone();

        for (UPAttribute attrConfig : parsedConfig.getAttributes()) {
            String attributeName = attrConfig.getName();
            List<AttributeValidatorMetadata> validators = new ArrayList<>();
            Map<String, Map<String, Object>> validationsConfig = attrConfig.getValidations();

            if (validationsConfig != null) {
                for (Map.Entry<String, Map<String, Object>> vc : validationsConfig.entrySet()) {
                    validators.add(createConfiguredValidator(vc.getKey(), vc.getValue()));
                }
            }

            UPAttributeRequired rc = attrConfig.getRequired();
            Predicate<AttributeContext> required = AttributeMetadata.ALWAYS_FALSE;

            if (rc != null && !(UserModel.USERNAME.equals(attributeName) || UserModel.EMAIL.equals(attributeName))) {
                // do not take requirements from config for username and email as they are
                // driven by business logic from parent!
                if (rc.isAlways() || UPConfigUtils.isRoleForContext(context, rc.getRoles())) {
                    required = AttributeMetadata.ALWAYS_TRUE;
                } else if (UPConfigUtils.canBeAuthFlowContext(context) && rc.getScopes() != null && !rc.getScopes().isEmpty()) {
                    // for contexts executed from auth flow and with configured scopes requirement
                    // we have to create required validation with scopes based selector
                    required = (c) -> createRequiredForScopePredicate(c, rc.getScopes());
                }

                validators.add(new AttributeValidatorMetadata(AttributeRequiredByMetadataValidator.ID));
            }

            Predicate<AttributeContext> writeAllowed = AttributeMetadata.ALWAYS_FALSE;
            Predicate<AttributeContext> readAllowed = AttributeMetadata.ALWAYS_FALSE;
            UPAttributePermissions permissions = attrConfig.getPermissions();

            if (permissions != null) {
                List<String> editRoles = permissions.getEdit();

                if (!editRoles.isEmpty()) {
                    writeAllowed = ac -> UPConfigUtils.isRoleForContext(ac.getContext(), editRoles);
                }

                List<String> viewRoles = permissions.getView();

                if (viewRoles.isEmpty()) {
                    readAllowed = writeAllowed;
                } else {
                    readAllowed = createViewAllowedPredicate(writeAllowed, viewRoles);
                }
            }

            Map<String, Object> annotations = attrConfig.getAnnotations();

            if (UserModel.USERNAME.equals(attributeName) || UserModel.EMAIL.equals(attributeName)) {
                if (permissions == null) {
                    writeAllowed = AttributeMetadata.ALWAYS_TRUE;
                }

                List<AttributeMetadata> atts = decoratedMetadata.getAttribute(attributeName);

                if (atts.isEmpty()) {
                    // attribute metadata doesn't exist so we have to add it. We keep it optional as Abstract base
                    // doesn't require it.
                    decoratedMetadata.addAttribute(attributeName, writeAllowed, validators).addAnnotations(annotations);
                } else {
                    // only add configured validators and annotations if attribute metadata exist
                    atts.stream().forEach(c -> c.addValidator(validators).addAnnotations(annotations));
                }
            } else {
                // always add validation for imuttable/read-only attributes
                validators.add(new AttributeValidatorMetadata(ImmutableAttributeValidator.ID));
                decoratedMetadata.addAttribute(attributeName, validators, writeAllowed, required, readAllowed).addAnnotations(annotations);
            }
        }

        return decoratedMetadata;

    }

    private Predicate<AttributeContext> createViewAllowedPredicate(Predicate<AttributeContext> canEdit,
            List<String> viewRoles) {
        return ac -> UPConfigUtils.isRoleForContext(ac.getContext(), viewRoles) || canEdit.test(ac);
    }

    /**
     * Get parsed config file configured in model. Default one used if not configured.
     *
     * @param model to take config from
     * @return parsed configuration
     */
    protected UPConfig getParsedConfig(ComponentModel model) {
        String rawConfig = getConfigJsonFromComponentModel(model);

        if (!isBlank(rawConfig)) {
            try {
                UPConfig upc = readConfig(new ByteArrayInputStream(rawConfig.getBytes("UTF-8")));

                //validate configuration to catch things like changed/removed validators etc, and warn early and clearly about this problem
                List<String> errors = UPConfigUtils.validate(session, upc);
                if (!errors.isEmpty()) {
                    throw new RuntimeException("UserProfile configuration for realm '" + session.getContext().getRealm().getName() + "' is invalid: " + errors.toString());
                }
                return upc;

            } catch (IOException e) {
                throw new RuntimeException("UserProfile configuration for realm '" + session.getContext().getRealm().getName() + "' is invalid:" + e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * Get componenet to store our "per realm" configuration into.
     *
     * @param session to be used, and take realm from
     * @return componenet
     */
    private ComponentModel getComponentModelOrCreate(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return realm.getComponentsStream(realm.getId(), UserProfileProvider.class.getName()).findAny().orElseGet(() -> realm.addComponentModel(new DeclarativeUserProfileModel()));
    }

    /**
     * Create validator for validation configured in the user profile config.
     *
     * @param validator id to create validator for
     * @param validatorConfig of the validator
     * @return validator metadata to run given validation
     */
    protected AttributeValidatorMetadata createConfiguredValidator(String validator, Map<String, Object> validatorConfig) {
        return new AttributeValidatorMetadata(validator, ValidatorConfig.builder().config(validatorConfig).config(AbstractSimpleValidator.IGNORE_EMPTY_VALUE, true).build());
    }

    private String getConfigJsonFromComponentModel(ComponentModel model) {
        if (model == null)
            return null;

        int count = model.get(UP_PIECES_COUNT_COMPONENT_CONFIG_KEY, 0);
        if (count < 1) {
            return defaultRawConfig;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            String v = model.get(UP_PIECE_COMPONENT_CONFIG_KEY_BASE + i);
            if (v != null)
                sb.append(v);
        }

        return sb.toString();
    }

    private void removeConfigJsonFromComponentModel(ComponentModel model) {
        if (model == null)
            return;

        int count = model.get(UP_PIECES_COUNT_COMPONENT_CONFIG_KEY, 0);
        if (count < 1) {
            return;
        }

        for (int i = 0; i < count; i++) {
            model.getConfig().remove(UP_PIECE_COMPONENT_CONFIG_KEY_BASE + i);
        }
        model.getConfig().remove(UP_PIECES_COUNT_COMPONENT_CONFIG_KEY);
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.DECLARATIVE_USER_PROFILE);
    }
}
