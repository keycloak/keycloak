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

package org.keycloak.protocol.saml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.crypto.dsig.CanonicalizationMethod;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.protocol.mappers.saml.OrganizationMembershipMapper;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.saml.validators.DestinationValidator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlProtocolFactory extends AbstractLoginProtocolFactory {

    public static final String SCOPE_ROLE_LIST = "role_list";
    private static final String ROLE_LIST_CONSENT_TEXT = "${samlRoleListScopeConsentText}";

    private DestinationValidator destinationValidator;

    @Override
    public Object createProtocolEndpoint(KeycloakSession session, EventBuilder event) {
        return new SamlService(session, event, destinationValidator);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new SamlProtocol().setSession(session);
    }

    @Override
    public void init(Config.Scope config) {
        //PicketLinkCoreSTS sts = PicketLinkCoreSTS.instance();
        //sts.installDefaultConfiguration();
        ProtocolMapperModel model;
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 email",
                "email",
                X500SAMLProfileConstants.EMAIL.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.EMAIL.getFriendlyName(),
                true, "${email}");
        builtins.put("X500 email", model);
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 givenName",
                "firstName",
                X500SAMLProfileConstants.GIVEN_NAME.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.GIVEN_NAME.getFriendlyName(),
                true, "${givenName}");
        builtins.put("X500 givenName", model);
        model = UserPropertyAttributeStatementMapper.createAttributeMapper("X500 surname",
                "lastName",
                X500SAMLProfileConstants.SURNAME.get(),
                JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get(),
                X500SAMLProfileConstants.SURNAME.getFriendlyName(),
                true, "${familyName}");
        builtins.put("X500 surname", model);
        model = RoleListMapper.create("role list", "Role", AttributeStatementHelper.BASIC, null, false);
        builtins.put("role list", model);
        defaultBuiltins.add(model);
        if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            model = OrganizationMembershipMapper.create();
            builtins.put("organization", model);
            defaultBuiltins.add(model);
        }
        this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
    }

    @Override
    public String getId() {
        return SamlProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    private Map<String, ProtocolMapperModel> builtins = new HashMap<>();
    private List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    @Override
    protected void createDefaultClientScopesImpl(RealmModel newRealm) {
        ClientScopeModel roleListScope = newRealm.addClientScope(SCOPE_ROLE_LIST);
        roleListScope.setDescription("SAML role list");
        roleListScope.setDisplayOnConsentScreen(true);
        roleListScope.setConsentScreenText(ROLE_LIST_CONSENT_TEXT);
        roleListScope.setProtocol(getId());
        roleListScope.addProtocolMapper(builtins.get("role list"));
        newRealm.addDefaultClientScope(roleListScope, true);
        if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            ClientScopeModel organizationScope = newRealm.addClientScope("saml_organization");
            organizationScope.setDescription("Organization Membership");
            organizationScope.setDisplayOnConsentScreen(false);
            organizationScope.setProtocol(getId());
            organizationScope.addProtocolMapper(builtins.get("organization"));
            newRealm.addDefaultClientScope(organizationScope, true);
        }
    }

    @Override
    protected void addDefaults(ClientModel client) {
    }

    @Override
    public void setupClientDefaults(ClientRepresentation clientRep, ClientModel newClient) {
        SamlRepresentationAttributes rep = new SamlRepresentationAttributes(clientRep.getAttributes());
        SamlClient client = new SamlClient(newClient);
        if (clientRep.isStandardFlowEnabled() == null) newClient.setStandardFlowEnabled(true);
        if (rep.getCanonicalizationMethod() == null) {
            client.setCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE);
        }
        if (rep.getSignatureAlgorithm() == null) {
            client.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        }

        if (rep.getNameIDFormat() == null) {
            client.setNameIDFormat("username");
        }

        if (rep.getIncludeAuthnStatement() == null) {
            client.setIncludeAuthnStatement(true);
        }

        if (rep.getForceNameIDFormat() == null) {
            client.setForceNameIDFormat(false);
        }

        if (rep.getAllowEcpFlow() == null) {
            client.setAllowECPFlow(false);
        }

        if (rep.getSamlServerSignature() == null) {
            client.setRequiresRealmSignature(true);
        }
        if (rep.getForcePostBinding() == null) {
            client.setForcePostBinding(true);
        }

        if (rep.getClientSignature() == null) {
            client.setRequiresClientSignature(true);
        }

        if (client.requiresClientSignature() && client.getClientSigningCertificate() == null
                && (!client.isUseMetadataDescriptorUrl() || client.getMetadataDescriptorUrl() != null)) {
            CertificateRepresentation info = KeycloakModelUtils.generateKeyPairCertificate(newClient.getClientId());
            client.setClientSigningCertificate(info.getCertificate());
            client.setClientSigningPrivateKey(info.getPrivateKey());

        }

        if (clientRep.isFrontchannelLogout() == null) {
            newClient.setFrontchannelLogout(true);
        }

        client.setArtifactBindingIdentifierFrom(clientRep.getClientId());
    }

    /**
     * defines the option-order in the admin-ui
     */
    @Override
    public int order() {
        return OIDCLoginProtocolFactory.UI_ORDER - 10;
    }
}
