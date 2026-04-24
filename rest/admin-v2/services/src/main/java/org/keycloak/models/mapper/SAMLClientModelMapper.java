/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

/**
 * Mapper for SAML clients between model and representation.
 */
public class SAMLClientModelMapper extends BaseClientModelMapper<SAMLClientRepresentation> {

    // SAML attribute keys
    private static final String SAML_NAME_ID_FORMAT = "saml_name_id_format";
    private static final String SAML_FORCE_NAME_ID_FORMAT = "saml_force_name_id_format";
    private static final String SAML_AUTHN_STATEMENT = "saml.authnstatement";
    private static final String SAML_SERVER_SIGNATURE = "saml.server.signature";
    private static final String SAML_ASSERTION_SIGNATURE = "saml.assertion.signature";
    private static final String SAML_CLIENT_SIGNATURE = "saml.client.signature";
    private static final String SAML_FORCE_POST_BINDING = "saml.force.post.binding";
    private static final String SAML_SIGNATURE_ALGORITHM = "saml.signature.algorithm";
    private static final String SAML_SIGNATURE_CANONICALIZATION = "saml_signature_canonicalization_method";
    private static final String SAML_SIGNING_CERTIFICATE = "saml.signing.certificate";
    private static final String SAML_ALLOW_ECP_FLOW = "saml.allow.ecp.flow";

    public SAMLClientModelMapper(KeycloakSession session) {
        super(session);
    }

    @Override
    protected SAMLClientRepresentation createClientRepresentation() {
        return new SAMLClientRepresentation();
    }

    @Override
    protected void fromModelSpecific(ClientModel model, SAMLClientRepresentation rep) {
        // Name ID settings
        rep.setNameIdFormat(model.getAttribute(SAML_NAME_ID_FORMAT));
        rep.setForceNameIdFormat(getBooleanAttribute(model, SAML_FORCE_NAME_ID_FORMAT));

        // Signature settings
        rep.setIncludeAuthnStatement(getBooleanAttribute(model, SAML_AUTHN_STATEMENT));
        rep.setSignDocuments(getBooleanAttribute(model, SAML_SERVER_SIGNATURE));
        rep.setSignAssertions(getBooleanAttribute(model, SAML_ASSERTION_SIGNATURE));
        rep.setClientSignatureRequired(getBooleanAttribute(model, SAML_CLIENT_SIGNATURE));
        rep.setSignatureAlgorithm(model.getAttribute(SAML_SIGNATURE_ALGORITHM));
        rep.setSignatureCanonicalizationMethod(model.getAttribute(SAML_SIGNATURE_CANONICALIZATION));
        rep.setSigningCertificate(model.getAttribute(SAML_SIGNING_CERTIFICATE));

        // Binding and logout settings
        rep.setForcePostBinding(getBooleanAttribute(model, SAML_FORCE_POST_BINDING));
        rep.setFrontChannelLogout(model.isFrontchannelLogout());

        // ECP flow
        rep.setAllowEcpFlow(getBooleanAttribute(model, SAML_ALLOW_ECP_FLOW));
    }

    @Override
    protected void toModelSpecific(SAMLClientRepresentation rep, ClientModel model) {
        model.setProtocol(SAMLClientRepresentation.PROTOCOL);

        // Name ID settings
        setAttributeIfNotNull(model, SAML_NAME_ID_FORMAT, rep.getNameIdFormat());
        setBooleanAttributeIfNotNull(model, SAML_FORCE_NAME_ID_FORMAT, rep.getForceNameIdFormat());

        // Signature settings
        setBooleanAttributeIfNotNull(model, SAML_AUTHN_STATEMENT, rep.getIncludeAuthnStatement());
        setBooleanAttributeIfNotNull(model, SAML_SERVER_SIGNATURE, rep.getSignDocuments());
        setBooleanAttributeIfNotNull(model, SAML_ASSERTION_SIGNATURE, rep.getSignAssertions());
        setBooleanAttributeIfNotNull(model, SAML_CLIENT_SIGNATURE, rep.getClientSignatureRequired());
        setAttributeIfNotNull(model, SAML_SIGNATURE_ALGORITHM, rep.getSignatureAlgorithm());
        setAttributeIfNotNull(model, SAML_SIGNATURE_CANONICALIZATION, rep.getSignatureCanonicalizationMethod());
        setAttributeIfNotNull(model, SAML_SIGNING_CERTIFICATE, rep.getSigningCertificate());

        // Binding and logout settings
        setBooleanAttributeIfNotNull(model, SAML_FORCE_POST_BINDING, rep.getForcePostBinding());
        if (rep.getFrontChannelLogout() != null) {
            model.setFrontchannelLogout(rep.getFrontChannelLogout());
        }

        // ECP flow
        setBooleanAttributeIfNotNull(model, SAML_ALLOW_ECP_FLOW, rep.getAllowEcpFlow());
    }

    private Boolean getBooleanAttribute(ClientModel model, String key) {
        String value = model.getAttribute(key);
        return value != null ? Boolean.parseBoolean(value) : null;
    }

    private void setAttributeIfNotNull(ClientModel model, String key, String value) {
        if (value != null) {
            model.setAttribute(key, value);
        }
    }

    private void setBooleanAttributeIfNotNull(ClientModel model, String key, Boolean value) {
        if (value != null) {
            model.setAttribute(key, value.toString());
        }
    }
}
