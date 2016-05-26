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

package org.keycloak.adapters.saml.config.parsers;

import org.keycloak.adapters.saml.DefaultSamlDeployment;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.KeycloakSamlAdapter;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.common.util.PemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DeploymentBuilder {
    public SamlDeployment build(InputStream xml, ResourceLoader resourceLoader) throws ParsingException {
        DefaultSamlDeployment deployment = new DefaultSamlDeployment();
        DefaultSamlDeployment.DefaultIDP idp = new DefaultSamlDeployment.DefaultIDP();
        DefaultSamlDeployment.DefaultSingleSignOnService sso = new DefaultSamlDeployment.DefaultSingleSignOnService();
        DefaultSamlDeployment.DefaultSingleLogoutService slo = new DefaultSamlDeployment.DefaultSingleLogoutService();
        idp.setSingleSignOnService(sso);
        idp.setSingleLogoutService(slo);

        KeycloakSamlAdapter adapter = (KeycloakSamlAdapter)(new KeycloakSamlAdapterXMLParser().parse(xml));
        SP sp = adapter.getSps().get(0);
        deployment.setConfigured(true);
        deployment.setEntityID(sp.getEntityID());
        deployment.setForceAuthentication(sp.isForceAuthentication());
        deployment.setIsPassive(sp.isIsPassive());
        deployment.setNameIDPolicyFormat(sp.getNameIDPolicyFormat());
        deployment.setLogoutPage(sp.getLogoutPage());
        deployment.setSignatureCanonicalizationMethod(sp.getIdp().getSignatureCanonicalizationMethod());
        deployment.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
        if (sp.getIdp().getSignatureAlgorithm() != null) {
            deployment.setSignatureAlgorithm(SignatureAlgorithm.valueOf(sp.getIdp().getSignatureAlgorithm()));
        }
        if (sp.getPrincipalNameMapping() != null) {
            SamlDeployment.PrincipalNamePolicy policy = SamlDeployment.PrincipalNamePolicy.valueOf(sp.getPrincipalNameMapping().getPolicy());
            deployment.setPrincipalNamePolicy(policy);
            deployment.setPrincipalAttributeName(sp.getPrincipalNameMapping().getAttributeName());
        }
        deployment.setRoleAttributeNames(sp.getRoleAttributes());
        if (sp.getRoleAttributes() == null) {
            Set<String> roles = new HashSet<>();
            roles.add("Role");
            deployment.setRoleAttributeNames(roles);
        }
        if (sp.getSslPolicy() != null) {
            SslRequired ssl = SslRequired.valueOf(sp.getSslPolicy());
            deployment.setSslRequired(ssl);
        }
        if (sp.getKeys() != null) {
            for (Key key : sp.getKeys()) {
                if (key.isSigning()) {
                    PrivateKey privateKey = null;
                    PublicKey publicKey = null;
                    if (key.getKeystore() != null) {
                        KeyStore keyStore = loadKeystore(resourceLoader, key);
                        Certificate cert = null;
                        try {
                            cert = keyStore.getCertificate(key.getKeystore().getCertificateAlias());
                            privateKey = (PrivateKey) keyStore.getKey(key.getKeystore().getPrivateKeyAlias(), key.getKeystore().getPrivateKeyPassword().toCharArray());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        publicKey = cert.getPublicKey();
                    } else {
                        if (key.getPrivateKeyPem() == null) {
                            throw new RuntimeException("SP signing key must have a PrivateKey defined");
                        }
                        try {
                            privateKey = PemUtils.decodePrivateKey(key.getPrivateKeyPem().trim());
                            if (key.getPublicKeyPem() == null && key.getCertificatePem() == null) {
                                throw new RuntimeException("Sp signing key must have a PublicKey or Certificate defined");
                            }
                            publicKey = getPublicKeyFromPem(key);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    KeyPair keyPair = new KeyPair(publicKey, privateKey);
                    deployment.setSigningKeyPair(keyPair);

                }
                if (key.isEncryption()) {
                    if (key.getKeystore() != null) {

                        KeyStore keyStore = loadKeystore(resourceLoader, key);
                        try {
                            PrivateKey privateKey = (PrivateKey) keyStore.getKey(key.getKeystore().getPrivateKeyAlias(), key.getKeystore().getPrivateKeyPassword().toCharArray());
                            deployment.setDecryptionKey(privateKey);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        if (key.getPrivateKeyPem() == null) {
                            throw new RuntimeException("SP signing key must have a PrivateKey defined");
                        }
                        try {
                            PrivateKey privateKey = PemUtils.decodePrivateKey(key.getPrivateKeyPem().trim());
                            deployment.setDecryptionKey(privateKey);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            }
        }

        deployment.setIdp(idp);
        idp.setEntityID(sp.getIdp().getEntityID());
        sso.setRequestBinding(SamlDeployment.Binding.parseBinding(sp.getIdp().getSingleSignOnService().getRequestBinding()));
        sso.setRequestBindingUrl(sp.getIdp().getSingleSignOnService().getBindingUrl());
        if (sp.getIdp().getSingleSignOnService().getResponseBinding() != null) {
            sso.setResponseBinding(SamlDeployment.Binding.parseBinding(sp.getIdp().getSingleSignOnService().getResponseBinding()));
        }
        sso.setSignRequest(sp.getIdp().getSingleSignOnService().isSignRequest());
        sso.setValidateResponseSignature(sp.getIdp().getSingleSignOnService().isValidateResponseSignature());
        sso.setValidateAssertionSignature(sp.getIdp().getSingleSignOnService().isValidateAssertionSignature());

        slo.setSignRequest(sp.getIdp().getSingleLogoutService().isSignRequest());
        slo.setSignResponse(sp.getIdp().getSingleLogoutService().isSignResponse());
        slo.setValidateResponseSignature(sp.getIdp().getSingleLogoutService().isValidateResponseSignature());
        slo.setValidateRequestSignature(sp.getIdp().getSingleLogoutService().isValidateRequestSignature());
        slo.setRequestBinding(SamlDeployment.Binding.parseBinding(sp.getIdp().getSingleLogoutService().getRequestBinding()));
        slo.setResponseBinding(SamlDeployment.Binding.parseBinding(sp.getIdp().getSingleLogoutService().getResponseBinding()));
        if (slo.getRequestBinding() == SamlDeployment.Binding.POST) {
            slo.setRequestBindingUrl(sp.getIdp().getSingleLogoutService().getPostBindingUrl());
        } else {
            slo.setRequestBindingUrl(sp.getIdp().getSingleLogoutService().getRedirectBindingUrl());
        }
        if (slo.getResponseBinding() == SamlDeployment.Binding.POST) {
            slo.setResponseBindingUrl(sp.getIdp().getSingleLogoutService().getPostBindingUrl());
        } else {
            slo.setResponseBindingUrl(sp.getIdp().getSingleLogoutService().getRedirectBindingUrl());
        }
        if (sp.getIdp().getKeys() != null) {
            for (Key key : sp.getIdp().getKeys()) {
                if (key.isSigning()) {
                    if (key.getKeystore() != null) {
                        KeyStore keyStore = loadKeystore(resourceLoader, key);
                        Certificate cert = null;
                        try {
                            cert = keyStore.getCertificate(key.getKeystore().getCertificateAlias());
                        } catch (KeyStoreException e) {
                            throw new RuntimeException(e);
                        }
                        idp.setSignatureValidationKey(cert.getPublicKey());
                    } else {
                        if (key.getPublicKeyPem() == null && key.getCertificatePem() == null) {
                            throw new RuntimeException("IDP signing key must have a PublicKey or Certificate defined");
                        }
                        try {
                            PublicKey publicKey = getPublicKeyFromPem(key);
                            idp.setSignatureValidationKey(publicKey);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }


        return deployment;
    }

    protected static PublicKey getPublicKeyFromPem(Key key) throws Exception {
        PublicKey publicKey;
        if (key.getPublicKeyPem() != null) {
            publicKey = PemUtils.decodePublicKey(key.getPublicKeyPem().trim());
        } else {
            Certificate cert = PemUtils.decodeCertificate(key.getCertificatePem().trim());
            publicKey = cert.getPublicKey();
        }
        return publicKey;
    }

    protected static KeyStore loadKeystore(ResourceLoader resourceLoader, Key key) {
        String type = key.getKeystore().getType();
        if (type == null) type = "JKS";
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(type);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        InputStream is = null;
        if (key.getKeystore().getFile() != null) {
            File fp = new File(key.getKeystore().getFile());
            if (!fp.exists()) {
            }
            try {
                is = new FileInputStream(fp);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("KeyStore " + key.getKeystore().getFile() + " does not exist");
            }

        } else {
            is = resourceLoader.getResourceAsStream(key.getKeystore().getResource());
            if (is == null) {
                throw new RuntimeException("KeyStore " + key.getKeystore().getResource() + " does not exist");
            }
        }
        try {
            keyStore.load(is, key.getKeystore().getPassword().toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return keyStore;
    }
}
