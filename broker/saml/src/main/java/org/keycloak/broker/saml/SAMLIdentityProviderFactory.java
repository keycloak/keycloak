/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.saml;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.picketlink.common.constants.JBossSAMLConstants;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.identity.federation.core.parsers.saml.SAMLParser;
import org.picketlink.identity.federation.saml.v2.metadata.EndpointType;
import org.picketlink.identity.federation.saml.v2.metadata.EntitiesDescriptorType;
import org.picketlink.identity.federation.saml.v2.metadata.EntityDescriptorType;
import org.picketlink.identity.federation.saml.v2.metadata.IDPSSODescriptorType;
import org.picketlink.identity.federation.saml.v2.metadata.KeyDescriptorType;
import org.picketlink.identity.federation.saml.v2.metadata.KeyTypes;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProviderFactory extends AbstractIdentityProviderFactory<SAMLIdentityProvider> {

    public static final String PROVIDER_ID = "saml";

    @Override
    public String getName() {
        return "SAML v2.0";
    }

    @Override
    public SAMLIdentityProvider create(IdentityProviderModel model) {
        return new SAMLIdentityProvider(new SAMLIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(InputStream inputStream) {
        try {
            Object parsedObject = new SAMLParser().parse(inputStream);
            EntityDescriptorType entityType;

            if (EntitiesDescriptorType.class.isInstance(parsedObject)) {
                entityType = (EntityDescriptorType) ((EntitiesDescriptorType) parsedObject).getEntityDescriptor().get(0);
            } else {
                entityType = (EntityDescriptorType) parsedObject;
            }

            List<EntityDescriptorType.EDTChoiceType> choiceType = entityType.getChoiceType();

            if (!choiceType.isEmpty()) {
                EntityDescriptorType.EDTChoiceType edtChoiceType = choiceType.get(0);
                List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = edtChoiceType.getDescriptors();

                if (!descriptors.isEmpty()) {
                    EntityDescriptorType.EDTDescriptorChoiceType edtDescriptorChoiceType = descriptors.get(0);
                    IDPSSODescriptorType idpDescriptor = edtDescriptorChoiceType.getIdpDescriptor();

                    if (idpDescriptor != null) {
                        SAMLIdentityProviderConfig samlIdentityProviderConfig = new SAMLIdentityProviderConfig();
                        String singleSignOnServiceUrl = null;
                        boolean postBinding = false;
                        for (EndpointType endpoint : idpDescriptor.getSingleSignOnService()) {
                            if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                                singleSignOnServiceUrl = endpoint.getLocation().toString();
                                postBinding = true;
                                break;
                            } else if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())){
                                singleSignOnServiceUrl = endpoint.getLocation().toString();
                            }
                        }
                        String singleLogoutServiceUrl = null;
                        for (EndpointType endpoint : idpDescriptor.getSingleLogoutService()) {
                            if (postBinding && endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                                singleLogoutServiceUrl = endpoint.getLocation().toString();
                                break;
                            } else if (!postBinding && endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())){
                                singleLogoutServiceUrl = endpoint.getLocation().toString();
                                break;
                            }

                        }
                        samlIdentityProviderConfig.setSingleLogoutServiceUrl(singleLogoutServiceUrl);
                        samlIdentityProviderConfig.setSingleSignOnServiceUrl(singleSignOnServiceUrl);
                        samlIdentityProviderConfig.setWantAuthnRequestsSigned(idpDescriptor.isWantAuthnRequestsSigned());
                        samlIdentityProviderConfig.setValidateSignature(idpDescriptor.isWantAuthnRequestsSigned());
                        samlIdentityProviderConfig.setPostBindingResponse(postBinding);
                        samlIdentityProviderConfig.setPostBindingAuthnRequest(postBinding);

                        List<KeyDescriptorType> keyDescriptor = idpDescriptor.getKeyDescriptor();
                        String defaultCertificate = null;

                        if (keyDescriptor != null) {
                            for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
                                Element keyInfo = keyDescriptorType.getKeyInfo();
                                Element x509KeyInfo = DocumentUtil.getChildElement(keyInfo, new QName("dsig", "X509Certificate"));

                                if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
                                    samlIdentityProviderConfig.setSigningCertificate(x509KeyInfo.getTextContent());
                                } else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
                                    samlIdentityProviderConfig.setEncryptionPublicKey(x509KeyInfo.getTextContent());
                                } else if (keyDescriptorType.getUse() ==  null) {
                                    defaultCertificate = x509KeyInfo.getTextContent();
                                }
                            }
                        }

                        if (defaultCertificate != null) {
                            if (samlIdentityProviderConfig.getSigningCertificate() == null) {
                                samlIdentityProviderConfig.setSigningCertificate(defaultCertificate);
                            }

                            if (samlIdentityProviderConfig.getEncryptionPublicKey() == null) {
                                samlIdentityProviderConfig.setEncryptionPublicKey(defaultCertificate);
                            }
                        }

                        return samlIdentityProviderConfig.getConfig();
                    }
                }
            }
        } catch (ParsingException pe) {
            throw new RuntimeException("Could not parse IdP SAML Metadata", pe);
        }

        return new HashMap<String, String>();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
