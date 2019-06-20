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

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.constants.KerberosConstants;
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
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.AllowedWebOriginsProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AudienceResolveProtocolMapper;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.protocol.oidc.mappers.UserRealmRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ServicesLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCLoginProtocolFactory extends AbstractLoginProtocolFactory {
    private static final Logger logger = Logger.getLogger(OIDCLoginProtocolFactory.class);

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
    // microprofile-jwt claims
    public static final String UPN = "upn";
    public static final String GROUPS = "groups";

    public static final String ROLES_SCOPE = "roles";
    public static final String WEB_ORIGINS_SCOPE = "web-origins";
    public static final String MICROPROFILE_JWT_SCOPE = "microprofile-jwt";

    public static final String PROFILE_SCOPE_CONSENT_TEXT = "${profileScopeConsentText}";
    public static final String EMAIL_SCOPE_CONSENT_TEXT = "${emailScopeConsentText}";
    public static final String ADDRESS_SCOPE_CONSENT_TEXT = "${addressScopeConsentText}";
    public static final String PHONE_SCOPE_CONSENT_TEXT = "${phoneScopeConsentText}";
    public static final String OFFLINE_ACCESS_SCOPE_CONSENT_TEXT = Constants.OFFLINE_ACCESS_SCOPE_CONSENT_TEXT;
    public static final String ROLES_SCOPE_CONSENT_TEXT = "${rolesScopeConsentText}";


    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OIDCLoginProtocol().setSession(session);
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    static Map<String, ProtocolMapperModel> builtins = new HashMap<>();

    static {
                ProtocolMapperModel model;
        model = UserPropertyMapper.createClaimMapper(USERNAME,
                "username",
                "preferred_username", "String",
                true, true);
        builtins.put(USERNAME, model);

        model = UserPropertyMapper.createClaimMapper(EMAIL,
                "email",
                "email", "String",
                true, true);
        builtins.put(EMAIL, model);

        model = UserPropertyMapper.createClaimMapper(GIVEN_NAME,
                "firstName",
                "given_name", "String",
                true, true);
        builtins.put(GIVEN_NAME, model);

        model = UserPropertyMapper.createClaimMapper(FAMILY_NAME,
                "lastName",
                "family_name", "String",
                true, true);
        builtins.put(FAMILY_NAME, model);

        createUserAttributeMapper(MIDDLE_NAME, "middleName", IDToken.MIDDLE_NAME, "String");
        createUserAttributeMapper(NICKNAME, "nickname", IDToken.NICKNAME, "String");
        createUserAttributeMapper(PROFILE_CLAIM, "profile", IDToken.PROFILE, "String");
        createUserAttributeMapper(PICTURE, "picture", IDToken.PICTURE, "String");
        createUserAttributeMapper(WEBSITE, "website", IDToken.WEBSITE, "String");
        createUserAttributeMapper(GENDER, "gender", IDToken.GENDER, "String");
        createUserAttributeMapper(BIRTHDATE, "birthdate", IDToken.BIRTHDATE, "String");
        createUserAttributeMapper(ZONEINFO, "zoneinfo", IDToken.ZONEINFO, "String");
        createUserAttributeMapper(UPDATED_AT, "updatedAt", IDToken.UPDATED_AT, "String");
        createUserAttributeMapper(LOCALE, "locale", IDToken.LOCALE, "String");

        createUserAttributeMapper(PHONE_NUMBER, "phoneNumber", IDToken.PHONE_NUMBER, "String");
        createUserAttributeMapper(PHONE_NUMBER_VERIFIED, "phoneNumberVerified", IDToken.PHONE_NUMBER_VERIFIED, "boolean");

        model = UserPropertyMapper.createClaimMapper(EMAIL_VERIFIED,
                "emailVerified",
                "email_verified", "boolean",
                true, true);
        builtins.put(EMAIL_VERIFIED, model);

        ProtocolMapperModel fullName = FullNameMapper.create(FULL_NAME, true, true, true);
        builtins.put(FULL_NAME, fullName);

        ProtocolMapperModel address = AddressMapper.createAddressMapper();
        builtins.put(ADDRESS, address);

        model = UserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                true, false);
        builtins.put(KerberosConstants.GSS_DELEGATION_CREDENTIAL, model);

        model = UserRealmRoleMappingMapper.create(null, REALM_ROLES, "realm_access.roles", true, false, true);
        builtins.put(REALM_ROLES, model);

        model = UserClientRoleMappingMapper.create(null, null, CLIENT_ROLES, "resource_access.${client_id}.roles", true, false, true);
        builtins.put(CLIENT_ROLES, model);

        model = AudienceResolveProtocolMapper.createClaimMapper(AUDIENCE_RESOLVE);
        builtins.put(AUDIENCE_RESOLVE, model);

        model = AllowedWebOriginsProtocolMapper.createClaimMapper(ALLOWED_WEB_ORIGINS);
        builtins.put(ALLOWED_WEB_ORIGINS, model);

        builtins.put(IMPERSONATOR_ID.getDisplayName(), UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_ID));
        builtins.put(IMPERSONATOR_USERNAME.getDisplayName(), UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_USERNAME));

        model = UserPropertyMapper.createClaimMapper(UPN, "username",
                "upn", "String",
                true, true);
        builtins.put(UPN, model);

        model = UserRealmRoleMappingMapper.create(null, GROUPS, GROUPS, true, true, true);
        builtins.put(GROUPS, model);
    }

    private static void createUserAttributeMapper(String name, String attrName, String claimName, String type) {
        ProtocolMapperModel model = UserAttributeMapper.createClaimMapper(name,
                attrName,
                claimName, type,
                true, true, false);
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
    }


    public static ClientScopeModel addRolesClientScope(RealmModel newRealm) {
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


    public static ClientScopeModel addWebOriginsClientScope(RealmModel newRealm) {
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
    public static ClientScopeModel addMicroprofileJWTClientScope(RealmModel newRealm) {
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

    @Override
    protected void addDefaults(ClientModel client) {
    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event) {
        return new OIDCLoginProtocolService(realm, event);
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
        if (rep.isBearerOnly() == null
                && rep.isPublicClient() == null) {
            newClient.setPublicClient(true);
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
    }

}
