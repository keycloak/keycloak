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

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.keycloak.models.ClientModel;
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

    @Override
    protected SAMLClientRepresentation createClientRepresentation() {
        return new SAMLClientRepresentation();
    }
    
    protected void addBooleanAttributeMapping(String name, String attribute, Function<SAMLClientRepresentation, Boolean> repGetter, BiConsumer<SAMLClientRepresentation, Boolean> repSetter) {
        this.addMapping(name, repGetter, repSetter, model -> getBooleanAttribute(model, attribute), (model, value) -> setBooleanAttributeIfNotNull(model, attribute, value));
    }
    
    protected void addAttributeMapping(String name, String attribute, Function<SAMLClientRepresentation, String> repGetter, BiConsumer<SAMLClientRepresentation, String> repSetter) {
        this.addMapping(name, repGetter, repSetter, model -> model.getAttribute(attribute), (model, value) -> setAttributeIfNotNull(model, attribute, value));
    }
    
    public SAMLClientModelMapper() {
        // Name ID settings
        addAttributeMapping("nameIdFormat", SAML_NAME_ID_FORMAT, SAMLClientRepresentation::getNameIdFormat, SAMLClientRepresentation::setNameIdFormat);
        addBooleanAttributeMapping("forceNameIdFormat", SAML_FORCE_NAME_ID_FORMAT, SAMLClientRepresentation::getForceNameIdFormat, SAMLClientRepresentation::setForceNameIdFormat);
        
        // Signature settings
        addBooleanAttributeMapping("includeAuthnStatement", SAML_AUTHN_STATEMENT, SAMLClientRepresentation::getIncludeAuthnStatement, SAMLClientRepresentation::setIncludeAuthnStatement);
        addBooleanAttributeMapping("signDocuments", SAML_SERVER_SIGNATURE, SAMLClientRepresentation::getSignDocuments, SAMLClientRepresentation::setSignDocuments);
        addBooleanAttributeMapping("signAssertions", SAML_ASSERTION_SIGNATURE, SAMLClientRepresentation::getSignAssertions, SAMLClientRepresentation::setSignAssertions);
        addBooleanAttributeMapping("clientSignatureRequired", SAML_CLIENT_SIGNATURE, SAMLClientRepresentation::getClientSignatureRequired, SAMLClientRepresentation::setClientSignatureRequired);
        addAttributeMapping("signatureAlgorithm", SAML_SIGNATURE_ALGORITHM, SAMLClientRepresentation::getSignatureAlgorithm, SAMLClientRepresentation::setSignatureAlgorithm);
        addAttributeMapping("signatureCanonicalizationMethod", SAML_SIGNATURE_CANONICALIZATION, SAMLClientRepresentation::getSignatureCanonicalizationMethod, SAMLClientRepresentation::setSignatureCanonicalizationMethod);
        addAttributeMapping("signingCertificate", SAML_SIGNING_CERTIFICATE, SAMLClientRepresentation::getSigningCertificate, SAMLClientRepresentation::setSigningCertificate);

        // Binding and logout settings
        addBooleanAttributeMapping("forcePostBinding", SAML_FORCE_POST_BINDING, SAMLClientRepresentation::getForcePostBinding, SAMLClientRepresentation::setForcePostBinding);
        // TODO: mapping from 3 value to 2 value boolean can be confusing from a patching perspective
        addMapping("frontChannelLogout", SAMLClientRepresentation::getFrontChannelLogout, SAMLClientRepresentation::setFrontChannelLogout, ClientModel::isFrontchannelLogout, (model, logout) -> model.setFrontchannelLogout(Boolean.TRUE.equals(logout)));

        // ECP flow
        addBooleanAttributeMapping("allowEcpFlow", SAML_ALLOW_ECP_FLOW, SAMLClientRepresentation::getAllowEcpFlow, SAMLClientRepresentation::setAllowEcpFlow);
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
