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
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.util.DocumentUtil;
import org.picketlink.identity.federation.core.parsers.saml.SAMLParser;
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

    @Override
    public String getName() {
        return "SAML v2.0";
    }

    @Override
    public SAMLIdentityProvider create(IdentityProviderModel model) {
        return new SAMLIdentityProvider(new SAMLIdentityProviderConfig(getId(), model.getId(), model.getName(), model.getConfig()));
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

                        samlIdentityProviderConfig.setSingleSignOnServiceUrl(idpDescriptor.getSingleSignOnService().get(0).getLocation().toString());
                        samlIdentityProviderConfig.setWantAuthnRequestsSigned(idpDescriptor.isWantAuthnRequestsSigned());
                        samlIdentityProviderConfig.setValidateSignature(idpDescriptor.isWantAuthnRequestsSigned());

                        List<KeyDescriptorType> keyDescriptor = idpDescriptor.getKeyDescriptor();
                        String defaultPublicKey = null;

                        if (keyDescriptor != null) {
                            for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
                                Element keyInfo = keyDescriptorType.getKeyInfo();
                                Element x509KeyInfo = DocumentUtil.getChildElement(keyInfo, new QName("dsig", "X509Certificate"));

                                if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
                                    samlIdentityProviderConfig.setSigningPublicKey(x509KeyInfo.getTextContent());
                                } else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
                                    samlIdentityProviderConfig.setEncryptionPublicKey(x509KeyInfo.getTextContent());
                                } else if (keyDescriptorType.getUse() ==  null) {
                                    defaultPublicKey = x509KeyInfo.getTextContent();
                                }
                            }
                        }

                        if (defaultPublicKey != null) {
                            if (samlIdentityProviderConfig.getSigningPublicKey() == null) {
                                samlIdentityProviderConfig.setSigningPublicKey(defaultPublicKey);
                            }

                            if (samlIdentityProviderConfig.getEncryptionPublicKey() == null) {
                                samlIdentityProviderConfig.setEncryptionPublicKey(defaultPublicKey);
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
        return "saml";
    }
}
