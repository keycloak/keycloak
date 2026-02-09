/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.saml.processing.core.saml.v2.util.SAMLMetadataUtil;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;

import org.jboss.logging.Logger;
import org.w3c.dom.Element;

/**
 * <p>PublicKeyLoader to retrieve keys from a SAML metadata entity endpoint.
 * It can be used to load IDP or SP keys. The abstract class does not
 * depend on keycloak session.</p>
 *
 * @author rmartinc
 */
public abstract class SamlAbstractMetadataPublicKeyLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(SamlAbstractMetadataPublicKeyLoader.class);
    private final boolean forIdP;

    public SamlAbstractMetadataPublicKeyLoader(boolean forIdP) {
        this.forIdP = forIdP;
    }

    protected abstract String getKeys() throws Exception;

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        String descriptor = getKeys();

        List<KeyDescriptorType> keyDescriptor = null;
        EntityDescriptorType entityType = SAMLMetadataUtil.parseEntityDescriptorType(descriptor);
        Long expirationTime = getExpirationTime(entityType::getValidUntil, entityType::getCacheDuration);
        if (forIdP) {
            IDPSSODescriptorType idpDescriptor = SAMLMetadataUtil.locateIDPSSODescriptorType(entityType);
            keyDescriptor = idpDescriptor != null? idpDescriptor.getKeyDescriptor() : null;
            if (idpDescriptor != null && expirationTime == null) {
                expirationTime = getExpirationTime(idpDescriptor::getValidUntil, idpDescriptor::getCacheDuration);
            }
        } else {
            SPSSODescriptorType spDescriptor = SAMLMetadataUtil.locateSPSSODescriptorType(entityType);
            keyDescriptor = spDescriptor != null? spDescriptor.getKeyDescriptor() : null;
            if (spDescriptor != null && expirationTime == null) {
                expirationTime = getExpirationTime(spDescriptor::getValidUntil, spDescriptor::getCacheDuration);
            }
        }

        List<KeyWrapper> keys = new ArrayList<>();
        if (keyDescriptor != null) {
            for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
                Element keyInfoElement = keyDescriptorType.getKeyInfo();
                if (keyInfoElement == null) {
                    continue;
                }

                KeyUse use = null; // TODO: default SIG? Or Both?
                if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
                    use = KeyUse.SIG;
                } else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
                    use = KeyUse.ENC;
                }

                try {
                    KeyInfo keyInfo = XMLSignatureUtil.createKeyInfo(keyInfoElement);

                    X509Certificate cert = null;
                    String kid = null;
                    for (XMLStructure xs : (List<XMLStructure>) keyInfo.getContent()) {
                        if (kid == null && xs instanceof KeyName) {
                            kid = ((KeyName) xs).getName();
                        } else if (cert == null && xs instanceof X509Data) {
                            for (Object content : ((X509Data) xs).getContent()) {
                                if (content instanceof X509Certificate) {
                                    cert = ((X509Certificate) content);
                                    // only the first X509Certificate is the signer
                                    // the rest are just part of the chain
                                    break;
                                }
                            }
                        }
                        // TODO: parse if KeyValue is defined without cert???
                        if (kid != null && cert != null) {
                            break;
                        }
                    }

                    if (cert != null) {
                        logger.debugf("Adding certificate %s to the list of public kets", cert.getSubjectX500Principal());
                        keys.add(createKeyWrapper(cert, kid, use));
                    }
                } catch (MarshalException e) {
                    logger.debugf(e, "Error parsing KeyInfo from metadata endpoint information");
                }
            }
        }

        return new PublicKeysWrapper(keys, expirationTime);
    }

    private Long getExpirationTime(Supplier<XMLGregorianCalendar> validUntil, Supplier<Duration> cacheDuration) {
        Long exp = null;
        final Duration cacheDurationValue = cacheDuration.get();
        if (cacheDurationValue != null) {
            final long now = Time.currentTimeMillis();
            exp = now + cacheDurationValue.getTimeInMillis(new Date(now));
        }

        final XMLGregorianCalendar validUntilValue = validUntil.get();
        if (validUntilValue != null) {
            exp = exp == null
                    ? validUntilValue.toGregorianCalendar().getTime().getTime()
                    : Math.min(exp, validUntilValue.toGregorianCalendar().getTime().getTime());
        }

        return exp;
    }

    private KeyWrapper createKeyWrapper(X509Certificate cert, String kid, KeyUse use) {
        KeyWrapper key = new KeyWrapper();
        key.setKid(kid != null? kid : cert.getSubjectX500Principal().getName());
        key.setAlgorithm(cert.getPublicKey().getAlgorithm());
        key.setUse(use);
        key.setType(cert.getPublicKey().getAlgorithm());
        key.setPublicKey(cert.getPublicKey());
        key.setCertificate(cert);
        return key;
    }
}
