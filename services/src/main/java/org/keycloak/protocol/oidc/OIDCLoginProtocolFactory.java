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

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCLoginProtocolFactory extends AbstractLoginProtocolFactory {
    private static ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email verified";
    public static final String GIVEN_NAME = "given name";
    public static final String FAMILY_NAME = "family name";
    public static final String FULL_NAME = "full name";
    public static final String LOCALE = "locale";
    public static final String USERNAME_CONSENT_TEXT = "${username}";
    public static final String EMAIL_CONSENT_TEXT = "${email}";
    public static final String EMAIL_VERIFIED_CONSENT_TEXT = "${emailVerified}";
    public static final String GIVEN_NAME_CONSENT_TEXT = "${givenName}";
    public static final String FAMILY_NAME_CONSENT_TEXT = "${familyName}";
    public static final String FULL_NAME_CONSENT_TEXT = "${fullName}";
    public static final String LOCALE_CONSENT_TEXT = "${locale}";


    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OIDCLoginProtocol().setSession(session);
    }

    @Override
    public List<ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    public List<ProtocolMapperModel> getDefaultBuiltinMappers() {
        return defaultBuiltins;
    }

    static List<ProtocolMapperModel> builtins = new ArrayList<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {

        ProtocolMapperModel model;
        model = UserPropertyMapper.createClaimMapper(USERNAME,
                "username",
                "preferred_username", "String",
                true, USERNAME_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.createClaimMapper(EMAIL,
                "email",
                "email", "String",
                true, EMAIL_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.createClaimMapper(GIVEN_NAME,
                "firstName",
                "given_name", "String",
                true, GIVEN_NAME_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.createClaimMapper(FAMILY_NAME,
                "lastName",
                "family_name", "String",
                true, FAMILY_NAME_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = UserPropertyMapper.createClaimMapper(EMAIL_VERIFIED,
                "emailVerified",
                "email_verified", "boolean",
                false, EMAIL_VERIFIED_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        model = UserAttributeMapper.createClaimMapper(LOCALE,
                "locale",
                "locale", "String",
                false, LOCALE_CONSENT_TEXT,
                true, true, false);
        builtins.add(model);

        ProtocolMapperModel fullName = new ProtocolMapperModel();
        fullName.setName(FULL_NAME);
        fullName.setProtocolMapper(FullNameMapper.PROVIDER_ID);
        fullName.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        fullName.setConsentRequired(true);
        fullName.setConsentText(FULL_NAME_CONSENT_TEXT);
        Map<String, String> config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        fullName.setConfig(config);
        builtins.add(fullName);
        defaultBuiltins.add(fullName);

        ProtocolMapperModel address = AddressMapper.createAddressMapper();
        builtins.add(address);

        model = UserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                true, "${gssDelegationCredential}",
                true, false);
        builtins.add(model);
    }

    @Override
    protected void addDefaults(ClientModel client) {
        for (ProtocolMapperModel model : defaultBuiltins) client.addProtocolMapper(model);
    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event) {
        return new OIDCLoginProtocolService(realm, event);
    }

    @Override
    public String getId() {
        return "openid-connect";
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
            logger.usingDeprecatedDirectGrantsOnly();
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

    @Override
    public void setupTemplateDefaults(ClientTemplateRepresentation clientRep, ClientTemplateModel newClient) {

    }
}
