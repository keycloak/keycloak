/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.protocol.oidc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.DefaultClientScopes;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationMembershipMapper;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.mappers.AcrProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.AllowedWebOriginsProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AudienceResolveProtocolMapper;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import org.keycloak.protocol.oidc.mappers.SubMapper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.protocol.oidc.mappers.UserRealmRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;

import org.jboss.logging.Logger;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;
import static org.keycloak.protocol.oidc.OIDCProviderConfig.DEFAULT_ADDITIONAL_REQ_PARAMS_FAIL_FAST;
import static org.keycloak.protocol.oidc.OIDCProviderConfig.DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_NUMBER;
import static org.keycloak.protocol.oidc.OIDCProviderConfig.DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_OVERALL_SIZE;
import static org.keycloak.protocol.oidc.OIDCProviderConfig.DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_SIZE;
import static org.keycloak.protocol.oidc.OIDCProviderConfig.DEFAULT_REQ_PARAMS_DEFAULT_MAX_SIZE;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCLoginProtocolFactory extends AbstractLoginProtocolFactory {
    private static final Logger logger = Logger.getLogger(OIDCLoginProtocolFactory.class);

    /**
     * determines the order in which the login protocols are displayed in the dropdown boxes in the UI
     */
    public static final int UI_ORDER = 100;

    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email verified";
    public static final String GIVEN_NAME = "given name";
    public static final String FAMILY_NAME = "family name";
    public static final String MIDDLE_NAME = "middle name";
    public static final String NICKNAME = "nickname";
    public static final String PROFILE_CLAIM = "profile";
    public static final String PICTURE = "picture";
    public static final String WEBSITE = "website";
    public static final String GENDER = "gender";
    public static final String BIRTHDATE = "birthdate";
    public static final String ZONEINFO = "zoneinfo";
    public static final String UPDATED_AT = "updated at";
    public static final String FULL_NAME = "full name";
    public static final String LOCALE = "locale";
    public static final String ADDRESS = "address";
    public static final String PHONE_NUMBER = "phone number";
    public static final String PHONE_NUMBER_VERIFIED = "phone number verified";
    public static final String REALM_ROLES = "realm roles";
    public static final String CLIENT_ROLES = "client roles";
    public static final String AUDIENCE_RESOLVE = "audience resolve";
    public static final String ALLOWED_WEB_ORIGINS = "allowed web origins";
    public static final String ACR = "acr loa level";
    public static final String ORGANIZATION = "organization";
    // microprofile-jwt claims
    public static final String UPN = "upn";
    public static final String GROUPS = "groups";

    public static final String ROLES_SCOPE = "roles";
    public static final String WEB_ORIGINS_SCOPE = "web-origins";
    public static final String MICROPROFILE_JWT_SCOPE = "microprofile-jwt";
    public static final String ACR_SCOPE = "acr";
    public static final String BASIC_SCOPE = "basic";

    public static final String PROFILE_SCOPE_CONSENT_TEXT = "${profileScopeConsentText}";
    public static final String EMAIL_SCOPE_CONSENT_TEXT = "${emailScopeConsentText}";
    public static final String ADDRESS_SCOPE_CONSENT_TEXT = "${addressScopeConsentText}";
    public static final String PHONE_SCOPE_CONSENT_TEXT = "${phoneScopeConsentText}";
    public static final String OFFLINE_ACCESS_SCOPE_CONSENT_TEXT = Constants.OFFLINE_ACCESS_SCOPE_CONSENT_TEXT;
    public static final String ROLES_SCOPE_CONSENT_TEXT = "${rolesScopeConsentText}";
    public static final String ORGANIZATION_SCOPE_CONSENT_TEXT = "${organizationScopeConsentText}";

    public static final String CONFIG_OIDC_REQ_PARAMS_DEFAULT_MAX_SIZE = "req-params-default-max-size";
    public static final String CONFIG_OIDC_REQ_PARAMS_MAX_SIZE_PREFIX = "req-params-max-size";
    public static final String CONFIG_OIDC_ADD_REQ_PARAMS_MAX_NUMBER = "add-req-params-max-number";
    public static final String CONFIG_OIDC_ADD_REQ_PARAMS_MAX_SIZE = "add-req-params-max-size";
    public static final String CONFIG_OIDC_ADD_REQ_PARAMS_MAX_OVERALL_SIZE = "add-req-params-max-overall-size";
    public static final String CONFIG_OIDC_ADD_REQ_PARAMS_FAIL_FAST = "add-req-params-fail-fast";

    /**
     * @deprecated To be removed in Keycloak 27
     */
    public static final String CONFIG_OIDC_ALLOW_MULTIPLE_AUDIENCES_FOR_JWT_CLIENT_AUTHENTICATION = "allow-multiple-audiences-for-jwt-client-authentication";

    private OIDCProviderConfig providerConfig;

    @Override
    public void init(Config.Scope config) {
        this.providerConfig = new OIDCProviderConfig(config);
        if (this.providerConfig.isAllowMultipleAudiencesForJwtClientAuthentication()) {
            logger.warnf("It is allowed to have multiple audiences for the JWT client authentication. This option is not recommended and will be removed in one of the future releases."
                    + " It is recommended to update your OAuth/OIDC clients to rather use single audience in the JWT token used for the client authentication.");
        }

        initBuiltIns();
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OIDCLoginProtocol(this.providerConfig).setSession(session);
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    private Map<String, ProtocolMapperModel> builtins = new HashMap<>();

    void initBuiltIns() {
        ProtocolMapperModel model;
        model = UserAttributeMapper.createClaimMapper(USERNAME,
                "username",
                "preferred_username", String.class.getSimpleName(),
                true, true, true);
        builtins.put(USERNAME, model);

        model = UserAttributeMapper.createClaimMapper(EMAIL,
                "email",
                "email", "String",
                true, true, true);
        builtins.put(EMAIL, model);

        model = UserAttributeMapper.createClaimMapper(GIVEN_NAME,
                "firstName",
                "given_name", "String",
                true, true, true);
        builtins.put(GIVEN_NAME, model);

        model = UserAttributeMapper.createClaimMapper(FAMILY_NAME,
                "lastName",
                "family_name", "String",
                true, true, true);
        builtins.put(FAMILY_NAME, model);

        createUserAttributeMapper(MIDDLE_NAME, "middleName", IDToken.MIDDLE_NAME, "String");
        createUserAttributeMapper(NICKNAME, "nickname", IDToken.NICKNAME, "String");
        createUserAttributeMapper(PROFILE_CLAIM, "profile", IDToken.PROFILE, "String");
        createUserAttributeMapper(PICTURE, "picture", IDToken.PICTURE, "String");
        createUserAttributeMapper(WEBSITE, "website", IDToken.WEBSITE, "String");
        createUserAttributeMapper(GENDER, "gender", IDToken.GENDER, "String");
        createUserAttributeMapper(BIRTHDATE, "birthdate", IDToken.BIRTHDATE, "String");
        createUserAttributeMapper(ZONEINFO, "zoneinfo", IDToken.ZONEINFO, "String");
        createUserAttributeMapper(UPDATED_AT, "updatedAt", IDToken.UPDATED_AT, "long");
        createUserAttributeMapper(LOCALE, "locale", IDToken.LOCALE, "String");

        createUserAttributeMapper(PHONE_NUMBER, "phoneNumber", IDToken.PHONE_NUMBER, "String");
        createUserAttributeMapper(PHONE_NUMBER_VERIFIED, "phoneNumberVerified", IDToken.PHONE_NUMBER_VERIFIED, "boolean");

        model = UserPropertyMapper.createClaimMapper(EMAIL_VERIFIED,
                "emailVerified",
                "email_verified", "boolean",
                true, true, true);
        builtins.put(EMAIL_VERIFIED, model);

        ProtocolMapperModel fullName = FullNameMapper.create(FULL_NAME, true, true, true, true);
        builtins.put(FULL_NAME, fullName);

        ProtocolMapperModel address = AddressMapper.createAddressMapper();
        builtins.put(ADDRESS, address);

        model = UserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                true, false, true);
        builtins.put(KerberosConstants.GSS_DELEGATION_CREDENTIAL, model);

        model = UserRealmRoleMappingMapper.create(null, REALM_ROLES, "realm_access.roles", true, false, true, true);
        builtins.put(REALM_ROLES, model);

        model = UserClientRoleMappingMapper.create(null, null, CLIENT_ROLES, "resource_access.${client_id}.roles", true, false, true, true);
        builtins.put(CLIENT_ROLES, model);

        model = AudienceResolveProtocolMapper.createClaimMapper(AUDIENCE_RESOLVE, true, true);
        builtins.put(AUDIENCE_RESOLVE, model);

        model = AllowedWebOriginsProtocolMapper.createClaimMapper(ALLOWED_WEB_ORIGINS, true, true);
        builtins.put(ALLOWED_WEB_ORIGINS, model);

        builtins.put(IMPERSONATOR_ID.getDisplayName(), UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_ID));
        builtins.put(IMPERSONATOR_USERNAME.getDisplayName(), UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_USERNAME));

        model = UserAttributeMapper.createClaimMapper(UPN, "username",
                "upn", "String",
                true, true, true);
        builtins.put(UPN, model);

        model = UserRealmRoleMappingMapper.create(null, GROUPS, GROUPS, true, true, true, true);
        builtins.put(GROUPS, model);

        if (Profile.isFeatureEnabled(Profile.Feature.STEP_UP_AUTHENTICATION)) {
            model = AcrProtocolMapper.create(ACR, true, true, true);
            builtins.put(ACR, model);
        }

        model = UserSessionNoteMapper.createClaimMapper(IDToken.AUTH_TIME, AuthenticationManager.AUTH_TIME,
                IDToken.AUTH_TIME, "long",
                true, true, false, true);
        builtins.put(IDToken.AUTH_TIME, model);

        model = SubMapper.create(IDToken.SUBJECT,true, true);
        builtins.put(IDToken.SUBJECT, model);
    }

    private void createUserAttributeMapper(String name, String attrName, String claimName, String type) {
        ProtocolMapperModel model = UserAttributeMapper.createClaimMapper(name,
                attrName,
                claimName, type,
                true, true, true, false);
        builtins.put(name, model);
    }

    @Override
    protected void createDefaultClientScopesImpl(RealmModel newRealm) {
        //name, family_name, given_name, middle_name, nickname, preferred_username, profile, picture, website, gender, birthdate, zoneinfo, locale, and updated_at.
        ClientScopeModel profileScope = newRealm.addClientScope(OAuth2Constants.SCOPE_PROFILE);
        profileScope.setDescription("OpenID Connect built-in scope: profile");
        profileScope.setDisplayOnConsentScreen(true);
        profileScope.setConsentScreenText(PROFILE_SCOPE_CONSENT_TEXT);
        profileScope.setIncludeInTokenScope(true);
        profileScope.setProtocol(getId());
        profileScope.addProtocolMapper(builtins.get(FULL_NAME));
        profileScope.addProtocolMapper(builtins.get(FAMILY_NAME));
        profileScope.addProtocolMapper(builtins.get(GIVEN_NAME));
        profileScope.addProtocolMapper(builtins.get(MIDDLE_NAME));
        profileScope.addProtocolMapper(builtins.get(NICKNAME));
        profileScope.addProtocolMapper(builtins.get(USERNAME));
        profileScope.addProtocolMapper(builtins.get(PROFILE_CLAIM));
        profileScope.addProtocolMapper(builtins.get(PICTURE));
        profileScope.addProtocolMapper(builtins.get(WEBSITE));
        profileScope.addProtocolMapper(builtins.get(GENDER));
        profileScope.addProtocolMapper(builtins.get(BIRTHDATE));
        profileScope.addProtocolMapper(builtins.get(ZONEINFO));
        profileScope.addProtocolMapper(builtins.get(LOCALE));
        profileScope.addProtocolMapper(builtins.get(UPDATED_AT));

        ClientScopeModel emailScope = newRealm.addClientScope(OAuth2Constants.SCOPE_EMAIL);
        emailScope.setDescription("OpenID Connect built-in scope: email");
        emailScope.setDisplayOnConsentScreen(true);
        emailScope.setConsentScreenText(EMAIL_SCOPE_CONSENT_TEXT);
        emailScope.setIncludeInTokenScope(true);
        emailScope.setProtocol(getId());
        emailScope.addProtocolMapper(builtins.get(EMAIL));
        emailScope.addProtocolMapper(builtins.get(EMAIL_VERIFIED));

        ClientScopeModel addressScope = newRealm.addClientScope(OAuth2Constants.SCOPE_ADDRESS);
        addressScope.setDescription("OpenID Connect built-in scope: address");
        addressScope.setDisplayOnConsentScreen(true);
        addressScope.setConsentScreenText(ADDRESS_SCOPE_CONSENT_TEXT);
        addressScope.setIncludeInTokenScope(true);
        addressScope.setProtocol(getId());
        addressScope.addProtocolMapper(builtins.get(ADDRESS));

        ClientScopeModel phoneScope = newRealm.addClientScope(OAuth2Constants.SCOPE_PHONE);
        phoneScope.setDescription("OpenID Connect built-in scope: phone");
        phoneScope.setDisplayOnConsentScreen(true);
        phoneScope.setConsentScreenText(PHONE_SCOPE_CONSENT_TEXT);
        phoneScope.setIncludeInTokenScope(true);
        phoneScope.setProtocol(getId());
        phoneScope.addProtocolMapper(builtins.get(PHONE_NUMBER));
        phoneScope.addProtocolMapper(builtins.get(PHONE_NUMBER_VERIFIED));

        // 'profile' and 'email' will be default scopes for now. 'address' and 'phone' will be optional scopes
        newRealm.addDefaultClientScope(profileScope, true);
        newRealm.addDefaultClientScope(emailScope, true);
        newRealm.addDefaultClientScope(addressScope, false);
        newRealm.addDefaultClientScope(phoneScope, false);

        RoleModel offlineRole = newRealm.getRole(OAuth2Constants.OFFLINE_ACCESS);
        if (offlineRole != null) {
            ClientScopeModel offlineAccessScope = KeycloakModelUtils.getClientScopeByName(newRealm, OAuth2Constants.OFFLINE_ACCESS);
            if (offlineAccessScope == null) {
                DefaultClientScopes.createOfflineAccessClientScope(newRealm, offlineRole);
            }
        }

        addRolesClientScope(newRealm);
        addWebOriginsClientScope(newRealm);
        addMicroprofileJWTClientScope(newRealm);
        addAcrClientScope(newRealm);
        addBasicClientScope(newRealm);
        addServiceAccountClientScope(newRealm);

        if (Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            ClientScopeModel organizationScope = newRealm.addClientScope(OAuth2Constants.ORGANIZATION);
            organizationScope.setDescription("Additional claims about the organization a subject belongs to");
            organizationScope.setDisplayOnConsentScreen(true);
            organizationScope.setConsentScreenText(ORGANIZATION_SCOPE_CONSENT_TEXT);
            organizationScope.setIncludeInTokenScope(true);
            organizationScope.setProtocol(getId());
            organizationScope.addProtocolMapper(OrganizationMembershipMapper.create(ORGANIZATION, true, true, true));
            newRealm.addDefaultClientScope(organizationScope, false);
        }
    }


    public ClientScopeModel addRolesClientScope(RealmModel newRealm) {
        ClientScopeModel rolesScope = KeycloakModelUtils.getClientScopeByName(newRealm, ROLES_SCOPE);
        if (rolesScope == null) {
            rolesScope = newRealm.addClientScope(ROLES_SCOPE);
            rolesScope.setDescription("OpenID Connect scope for add user roles to the access token");
            rolesScope.setDisplayOnConsentScreen(true);
            rolesScope.setConsentScreenText(ROLES_SCOPE_CONSENT_TEXT);
            rolesScope.setIncludeInTokenScope(false);
            rolesScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            rolesScope.addProtocolMapper(builtins.get(REALM_ROLES));
            rolesScope.addProtocolMapper(builtins.get(CLIENT_ROLES));
            rolesScope.addProtocolMapper(builtins.get(AUDIENCE_RESOLVE));

            // 'roles' will be default client scope
            newRealm.addDefaultClientScope(rolesScope, true);
        } else {
            logger.debugf("Client scope '%s' already exists in realm '%s'. Skip creating it.", ROLES_SCOPE, newRealm.getName());
        }

        return rolesScope;
    }


    public ClientScopeModel addWebOriginsClientScope(RealmModel newRealm) {
        ClientScopeModel originsScope = KeycloakModelUtils.getClientScopeByName(newRealm, WEB_ORIGINS_SCOPE);
        if (originsScope == null) {
            originsScope = newRealm.addClientScope(WEB_ORIGINS_SCOPE);
            originsScope.setDescription("OpenID Connect scope for add allowed web origins to the access token");
            originsScope.setDisplayOnConsentScreen(false); // No requesting consent from user for this. It is rather the permission of client
            originsScope.setConsentScreenText("");
            originsScope.setIncludeInTokenScope(false);
            originsScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            originsScope.addProtocolMapper(builtins.get(ALLOWED_WEB_ORIGINS));

            // 'web-origins' will be default client scope
            newRealm.addDefaultClientScope(originsScope, true);
        } else {
            logger.debugf("Client scope '%s' already exists in realm '%s'. Skip creating it.", WEB_ORIGINS_SCOPE, newRealm.getName());
        }

        return originsScope;
    }

    /**
     * Adds the {@code microprofile-jwt} optional client scope to the specified realm. If a {@code microprofile-jwt} client scope
     * already exists in the realm then the existing scope is returned. Otherwise, a new scope is created and returned.
     *
     * @param newRealm the realm to which the {@code microprofile-jwt} scope is to be added.
     * @return a reference to the {@code microprofile-jwt} client scope that was either created or already exists in the realm.
     */
    public ClientScopeModel addMicroprofileJWTClientScope(RealmModel newRealm) {
        ClientScopeModel microprofileScope = KeycloakModelUtils.getClientScopeByName(newRealm, MICROPROFILE_JWT_SCOPE);
        if (microprofileScope == null) {
            microprofileScope = newRealm.addClientScope(MICROPROFILE_JWT_SCOPE);
            microprofileScope.setDescription("Microprofile - JWT built-in scope");
            microprofileScope.setDisplayOnConsentScreen(false);
            microprofileScope.setIncludeInTokenScope(true);
            microprofileScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            microprofileScope.addProtocolMapper(builtins.get(UPN));
            microprofileScope.addProtocolMapper(builtins.get(GROUPS));
            newRealm.addDefaultClientScope(microprofileScope, false);
        } else {
            logger.debugf("Client scope '%s' already exists in realm '%s'. Skip creating it.", MICROPROFILE_JWT_SCOPE, newRealm.getName());
        }

        return microprofileScope;
    }


    public ClientScopeModel addAcrClientScope(RealmModel newRealm) {
        if (Profile.isFeatureEnabled(Profile.Feature.STEP_UP_AUTHENTICATION)) {
            ClientScopeModel acrScope = KeycloakModelUtils.getClientScopeByName(newRealm, ACR_SCOPE);
            if (acrScope == null) {
                acrScope = newRealm.addClientScope(ACR_SCOPE);
                acrScope.setDescription("OpenID Connect scope for add acr (authentication context class reference) to the token");
                acrScope.setDisplayOnConsentScreen(false);
                acrScope.setIncludeInTokenScope(false);
                acrScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
                acrScope.addProtocolMapper(builtins.get(ACR));

                // acr will be realm 'default' client scope
                newRealm.addDefaultClientScope(acrScope, true);

                logger.debugf("Client scope '%s' created in the realm '%s'.", ACR_SCOPE, newRealm.getName());
            } else {
                logger.debugf("Client scope '%s' already exists in realm '%s'. Skip creating it.", ACR_SCOPE, newRealm.getName());
            }
            return acrScope;
        } else {
            logger.debugf("Skip creating client scope '%s' in the realm '%s' due the step-up authentication feature is disabled.", ACR_SCOPE, newRealm.getName());
            return null;
        }
    }

    public ClientScopeModel addBasicClientScope(RealmModel newRealm) {
        ClientScopeModel basicScope = KeycloakModelUtils.getClientScopeByName(newRealm, BASIC_SCOPE);
        if (basicScope == null) {
            basicScope = newRealm.addClientScope(BASIC_SCOPE);
            basicScope.setDescription("OpenID Connect scope for add all basic claims to the token");
            basicScope.setDisplayOnConsentScreen(false);
            basicScope.setIncludeInTokenScope(false);
            basicScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            basicScope.addProtocolMapper(builtins.get(IDToken.AUTH_TIME));
            basicScope.addProtocolMapper(builtins.get(IDToken.SUBJECT));

            newRealm.addDefaultClientScope(basicScope, true);

            logger.debugf("Client scope '%s' created in the realm '%s'.", BASIC_SCOPE, newRealm.getName());
        } else {
            logger.debugf("Client scope '%s' already exists in realm '%s'. Skip creating it.", BASIC_SCOPE, newRealm.getName());
        }
        return basicScope;
    }

    public ClientScopeModel addServiceAccountClientScope(RealmModel newRealm) {
        ClientScopeModel serviceAccountScope = KeycloakModelUtils.getClientScopeByName(newRealm, ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE);
        if (serviceAccountScope == null) {
            serviceAccountScope = newRealm.addClientScope(ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE);
            serviceAccountScope.setDescription("Specific scope for a client enabled for service accounts");
            serviceAccountScope.setDisplayOnConsentScreen(false);
            serviceAccountScope.setIncludeInTokenScope(false);
            serviceAccountScope.setProtocol(getId());
            serviceAccountScope.addProtocolMapper(UserSessionNoteMapper.createClaimMapper(ServiceAccountConstants.CLIENT_ID_PROTOCOL_MAPPER,
                    ServiceAccountConstants.CLIENT_ID,
                    ServiceAccountConstants.CLIENT_ID, "String",
                    true, true, true));
            serviceAccountScope.addProtocolMapper(UserSessionNoteMapper.createClaimMapper(ServiceAccountConstants.CLIENT_HOST_PROTOCOL_MAPPER,
                    ServiceAccountConstants.CLIENT_HOST,
                    ServiceAccountConstants.CLIENT_HOST, "String",
                    true, true, true));
            serviceAccountScope.addProtocolMapper(UserSessionNoteMapper.createClaimMapper(ServiceAccountConstants.CLIENT_ADDRESS_PROTOCOL_MAPPER,
                    ServiceAccountConstants.CLIENT_ADDRESS,
                    ServiceAccountConstants.CLIENT_ADDRESS, "String",
                    true, true, true));

            logger.debugf("Client scope '%s' created in the realm '%s'.", ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE, newRealm.getName());
        } else {
            logger.debugf("Client scope '%s' already exists in realm '%s'. Skip creating it.", ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE, newRealm.getName());
        }

        return serviceAccountScope;
    }


    @Override
    protected void addDefaults(ClientModel client) {
    }

    @Override
    public Object createProtocolEndpoint(KeycloakSession session, EventBuilder event) {
        return new OIDCLoginProtocolService(session, event);
    }

    @Override
    public String getId() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public void setupClientDefaults(ClientRepresentation rep, ClientModel newClient) {
        if (rep.getRootUrl() != null && (rep.getRedirectUris() == null || rep.getRedirectUris().isEmpty())) {
            String root = rep.getRootUrl();
            if (root.endsWith("/")) root = root + "*";
            else root = root + "/*";
            newClient.addRedirectUri(root);

            Set<String> origins = new HashSet<String>();
            String origin = UriUtils.getOrigin(root);
            logger.debugv("adding default client origin: {0}" , origin);
            origins.add(origin);
            newClient.setWebOrigins(origins);
        }
        // if no client type provided, default to public client
        if (rep.isBearerOnly() == null
                && rep.isPublicClient() == null) {
            newClient.setPublicClient(true);
            newClient.setSecret(null);
        } else if (!(Boolean.TRUE.equals(rep.isBearerOnly()) || Boolean.TRUE.equals(rep.isPublicClient()))) {
            // if client is confidential, generate a secret if none is defined
            if (newClient.getSecret() == null) {
                KeycloakModelUtils.generateSecret(newClient);
            }
        }
        if (rep.isBearerOnly() == null) newClient.setBearerOnly(false);
        if (rep.getAdminUrl() == null && rep.getRootUrl() != null) {
            newClient.setManagementUrl(rep.getRootUrl());
        }


        // Backwards compatibility only
        if (rep.isDirectGrantsOnly() != null) {
            ServicesLogger.LOGGER.usingDeprecatedDirectGrantsOnly();
            newClient.setStandardFlowEnabled(!rep.isDirectGrantsOnly());
            newClient.setDirectAccessGrantsEnabled(rep.isDirectGrantsOnly());
        } else {
            if (rep.isStandardFlowEnabled() == null) newClient.setStandardFlowEnabled(true);
            if (rep.isDirectAccessGrantsEnabled() == null) newClient.setDirectAccessGrantsEnabled(true);

        }

        if (rep.isImplicitFlowEnabled() == null) newClient.setImplicitFlowEnabled(false);
        if (rep.isPublicClient() == null) newClient.setPublicClient(true);
        if (rep.isFrontchannelLogout() == null) newClient.setFrontchannelLogout(false);
        if (OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).getBackchannelLogoutUrl() == null){
            OIDCAdvancedConfigWrapper oidcAdvancedConfigWrapper = OIDCAdvancedConfigWrapper.fromClientModel(newClient);
            oidcAdvancedConfigWrapper.setBackchannelLogoutSessionRequired(true);
            oidcAdvancedConfigWrapper.setBackchannelLogoutRevokeOfflineTokens(false);
        }
    }

    /**
     * defines the option-order in the admin-ui
     */
    @Override
    public int order() {
        return UI_ORDER;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(CONFIG_OIDC_REQ_PARAMS_DEFAULT_MAX_SIZE)
                    .type("int")
                    .helpText("Maximum default length of the standard OIDC parameter sent to the OIDC authentication request. This applies to most of the standard parameters like for example 'state', 'nonce' etc." +
                            " The exception is 'login_hint' parameter, which has maximum length of 255 characters.")
                    .defaultValue(DEFAULT_REQ_PARAMS_DEFAULT_MAX_SIZE)
                    .add()
                .property()
                    .name(CONFIG_OIDC_REQ_PARAMS_MAX_SIZE_PREFIX + "--" + OIDCLoginProtocol.LOGIN_HINT_PARAM)
                    .type("int")
                    .helpText("Maximum length of the standard OIDC authentication request parameter overriden for the specified parameter. Useful if some standard OIDC parameter should have different limit than '" + CONFIG_OIDC_REQ_PARAMS_DEFAULT_MAX_SIZE +
                            "'. It is needed to add the name of the parameter after this prefix into the configuration. In this example, the '" + OIDCLoginProtocol.LOGIN_HINT_PARAM + "' parameter is used, but this format is supported for any known standard OIDC/OAuth2 parameter.")
                    .add()
                .property()
                    .name(CONFIG_OIDC_ADD_REQ_PARAMS_MAX_NUMBER)
                    .type("int")
                    .helpText("Maximum number of additional request parameters sent to the OIDC authentication request. As 'additional request parameter' is meant some custom parameter not directly treated as standard OIDC/OAuth2 protocol parameter. Additional parameters might be useful for example to add custom claims to the OIDC token (in case that also particular protocol mappers are configured).")
                    .defaultValue(DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_NUMBER)
                    .add()
                .property()
                    .name(CONFIG_OIDC_ADD_REQ_PARAMS_MAX_SIZE)
                    .type("int")
                    .helpText("Maximum size of single additional request parameter value See '" + CONFIG_OIDC_ADD_REQ_PARAMS_MAX_NUMBER + "' for more details about additional request parameters")
                    .defaultValue(DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_SIZE)
                    .add()
                .property()
                    .name(CONFIG_OIDC_ADD_REQ_PARAMS_MAX_OVERALL_SIZE)
                    .type("int")
                    .helpText("Maximum size of all additional request parameters values together. See '" + CONFIG_OIDC_ADD_REQ_PARAMS_MAX_NUMBER + "' for more details about additional request parameters")
                    .defaultValue(DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_OVERALL_SIZE)
                    .add()
                .property()
                    .name(CONFIG_OIDC_ADD_REQ_PARAMS_FAIL_FAST)
                    .type("boolean")
                    .helpText("Whether the fail-fast strategy should be enforced in case if the limit for some standard OIDC parameter or additional OIDC parameter is not met for the parameters sent to the OIDC authentication request." +
                            " If false, then all additional request parameters to not meet the configuration are silently ignored. If true, an exception will be raised and OIDC authentication request will not be allowed.")
                    .defaultValue(DEFAULT_ADDITIONAL_REQ_PARAMS_FAIL_FAST)
                    .add()
                .build();
    }
}
