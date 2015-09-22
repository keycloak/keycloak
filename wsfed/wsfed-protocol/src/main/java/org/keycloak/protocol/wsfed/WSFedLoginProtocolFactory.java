/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.wsfed;

import org.keycloak.protocol.wsfed.mappers.OIDCFullNameMapper;
import org.keycloak.protocol.wsfed.mappers.OIDCUserPropertyMapper;
import org.keycloak.protocol.wsfed.mappers.SAMLRoleListMapper;
import org.keycloak.protocol.wsfed.mappers.SAMLUserPropertyAttributeStatementMapper;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 5/19/15.
 */
public class WSFedLoginProtocolFactory extends AbstractLoginProtocolFactory {
    public static final String USERNAME = "username";
    public static final String UPN = "upn";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email verified";
    public static final String GIVEN_NAME = "given name";
    public static final String FAMILY_NAME = "family name";
    public static final String FULL_NAME = "full name";
    public static final String USERNAME_CONSENT_TEXT = "${username}";
    public static final String UPN_CONSENT_TEXT = "${upn}";
    public static final String EMAIL_CONSENT_TEXT = "${email}";
    public static final String EMAIL_VERIFIED_CONSENT_TEXT = "${emailVerified}";
    public static final String GIVEN_NAME_CONSENT_TEXT = "${givenName}";
    public static final String FAMILY_NAME_CONSENT_TEXT = "${familyName}";
    public static final String FULL_NAME_CONSENT_TEXT = "${fullName}";

    static List<ProtocolMapperModel> builtins = new ArrayList<>();
    static List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        return new WSFedService(realm, event, authManager);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new WSFedLoginProtocol().setSession(session);
    }

    @Override
    public String getId() {
        return "wsfed";
    }

    @Override
    public List<ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    public List<ProtocolMapperModel> getDefaultBuiltinMappers() {
        return defaultBuiltins;
    }

    static {
        ProtocolMapperModel model;

        //OIDC
        model = OIDCUserPropertyMapper.createClaimMapper(UPN,
                "username",
                "upn", "String",
                true, UPN_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserPropertyMapper.createClaimMapper(USERNAME,
                "username",
                "preferred_username", "String",
                true, USERNAME_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserPropertyMapper.createClaimMapper(EMAIL,
                "email",
                "email", "String",
                true, EMAIL_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserPropertyMapper.createClaimMapper(GIVEN_NAME,
                "firstName",
                "given_name", "String",
                true, GIVEN_NAME_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserPropertyMapper.createClaimMapper(FAMILY_NAME,
                "lastName",
                "family_name", "String",
                true, FAMILY_NAME_CONSENT_TEXT,
                true, true);
        builtins.add(model);
        defaultBuiltins.add(model);
        model = OIDCUserPropertyMapper.createClaimMapper(EMAIL_VERIFIED,
                "emailVerified",
                "email_verified", "boolean",
                false, EMAIL_VERIFIED_CONSENT_TEXT,
                true, true);
        builtins.add(model);

        model = OIDCFullNameMapper.create(FULL_NAME, true, FULL_NAME_CONSENT_TEXT, true, true);
        builtins.add(model);
        defaultBuiltins.add(model);

        ProtocolMapperModel address = AddressMapper.createAddressMapper();
        builtins.add(address);

        //SAML
        model = SAMLUserPropertyAttributeStatementMapper.createAttributeMapper("X500 email",
                "email",
                X500SAMLProfileConstants.EMAIL.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.EMAIL.getFriendlyName(),
                true, "${email}");
        builtins.add(model);
        model = SAMLUserPropertyAttributeStatementMapper.createAttributeMapper("X500 givenName",
                "firstName",
                X500SAMLProfileConstants.GIVEN_NAME.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(),
                true, "${givenName}");
        builtins.add(model);
        model = SAMLUserPropertyAttributeStatementMapper.createAttributeMapper("X500 surname",
                "lastName",
                X500SAMLProfileConstants.SURNAME.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.SURNAME.getFriendlyName(),
                true, "${familyName}");
        builtins.add(model);
        model = SAMLRoleListMapper.create("saml role list", "Role", AttributeStatementHelper.BASIC, null, false);
        builtins.add(model);
        defaultBuiltins.add(model);

    }

    @Override
    protected void addDefaults(ClientModel client) {
        for (ProtocolMapperModel model : defaultBuiltins) client.addProtocolMapper(model);
    }
}
