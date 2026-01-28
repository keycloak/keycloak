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

package org.keycloak.services.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.http.FormPartValue;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CertificateInfoHelper {

    public static final String CERTIFICATE_PEM = "Certificate PEM";
    public static final String PUBLIC_KEY_PEM = "Public Key PEM";
    public static final String JSON_WEB_KEY_SET = "JSON Web Key Set";

    private static final Logger logger = Logger.getLogger(CertificateInfoHelper.class);

    public static final String PRIVATE_KEY = "private.key";
    public static final String X509CERTIFICATE = "certificate";
    public static final String PUBLIC_KEY = "public.key";

    public static final String KID = "kid";


    // CLIENT MODEL METHODS

    public static CertificateRepresentation getCertificateFromClient(ClientModel client, String attributePrefix) {
        String privateKeyAttribute = attributePrefix + "." + PRIVATE_KEY;
        String certificateAttribute = attributePrefix + "." + X509CERTIFICATE;
        String publicKeyAttribute = attributePrefix + "." + PUBLIC_KEY;
        String kidAttribute = attributePrefix + "." + KID;

        if (OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())
                && Boolean.parseBoolean(client.getAttribute(OIDCConfigAttributes.USE_JWKS_STRING))) {
            return jwksStringToSigCertificateRepresentation(client.getAttribute(OIDCConfigAttributes.JWKS_STRING));
        }

        CertificateRepresentation rep = new CertificateRepresentation();
        rep.setCertificate(client.getAttribute(certificateAttribute));
        rep.setPublicKey(client.getAttribute(publicKeyAttribute));
        rep.setPrivateKey(client.getAttribute(privateKeyAttribute));
        rep.setKid(client.getAttribute(kidAttribute));

        return rep;
    }

    public static CertificateRepresentation jwksStringToSigCertificateRepresentation(String jwks) {
        if (jwks == null) {
            throw new IllegalStateException("The jwks is null!");
        }

        try {
            JSONWebKeySet keySet = JsonSerialization.readValue(jwks, JSONWebKeySet.class);
            if (keySet == null || keySet.getKeys() == null) {
                throw new IllegalStateException("Certificate not found");
            }
            JWK publicKeyJwk = JWKSUtils.getKeyForUse(keySet, JWK.Use.SIG);
            if (publicKeyJwk == null) {
                throw new IllegalStateException("Certificate not found for use sig");
            }

            // set the public key as before and also the full jwks
            PublicKey publicKey = JWKParser.create(publicKeyJwk).toPublicKey();
            String publicKeyPem = KeycloakModelUtils.getPemFromKey(publicKey);
            CertificateRepresentation info = new CertificateRepresentation();
            info.setJwks(jwks);
            info.setPublicKey(publicKeyPem);
            info.setKid(publicKeyJwk.getKeyId());
            return info;
        } catch (IOException e) {
            throw new IllegalStateException("Invalid jwks representation!", e);
        }
    }

    public static void updateClientModelCertificateInfo(ClientModel client, CertificateRepresentation rep, String attributePrefix) {
        if (rep.getPublicKey() == null && rep.getCertificate() == null) {
            throw new IllegalStateException("Both certificate and publicKey are null!");
        }

        if (rep.getPublicKey() != null && rep.getCertificate() != null) {
            throw new IllegalStateException("Both certificate and publicKey are not null!");
        }

        String privateKeyAttribute = attributePrefix + "." + PRIVATE_KEY;
        String certificateAttribute = attributePrefix + "." + X509CERTIFICATE;
        String publicKeyAttribute = attributePrefix + "." + PUBLIC_KEY;
        String kidAttribute = attributePrefix + "." + KID;

        setOrRemoveAttr(client, privateKeyAttribute, rep.getPrivateKey());
        setOrRemoveAttr(client, publicKeyAttribute, rep.getPublicKey());
        setOrRemoveAttr(client, certificateAttribute, rep.getCertificate());
        setOrRemoveAttr(client, kidAttribute, rep.getKid());

        if (OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            setOrRemoveAttr(client, OIDCConfigAttributes.USE_JWKS_STRING, null);
            setOrRemoveAttr(client, OIDCConfigAttributes.JWKS_STRING, null);
        }
    }

    public static void updateClientModelJwksString(ClientModel client, String attributePrefix, String jwks) {
        if (jwks == null) {
            throw new IllegalStateException("jwks string is null!");
        }

        if (!OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            throw new IllegalStateException("jwks can only be set for OIDC clients!");
        }

        String privateKeyAttribute = attributePrefix + "." + PRIVATE_KEY;
        String certificateAttribute = attributePrefix + "." + X509CERTIFICATE;
        String publicKeyAttribute = attributePrefix + "." + PUBLIC_KEY;
        String kidAttribute = attributePrefix + "." + KID;

        setOrRemoveAttr(client, privateKeyAttribute, null);
        setOrRemoveAttr(client, publicKeyAttribute, null);
        setOrRemoveAttr(client, certificateAttribute, null);
        setOrRemoveAttr(client, kidAttribute, null);
        setOrRemoveAttr(client, OIDCConfigAttributes.USE_JWKS_STRING, Boolean.TRUE.toString());
        setOrRemoveAttr(client, OIDCConfigAttributes.JWKS_STRING, jwks);
    }

    private static void setOrRemoveAttr(ClientModel client, String attrName, String attrValue) {
        if (attrValue != null) {
            client.setAttribute(attrName, attrValue);
        } else {
            client.removeAttribute(attrName);
        }
    }


    // CLIENT REPRESENTATION METHODS

    public static void updateClientRepresentationCertificateInfo(ClientRepresentation client, CertificateRepresentation rep, String attributePrefix) {
        String privateKeyAttribute = attributePrefix + "." + PRIVATE_KEY;
        String certificateAttribute = attributePrefix + "." + X509CERTIFICATE;
        String publicKeyAttribute = attributePrefix + "." + PUBLIC_KEY;
        String kidAttribute = attributePrefix + "." + KID;

        if (rep.getPublicKey() == null && rep.getCertificate() == null) {
            throw new IllegalStateException("Both certificate and publicKey are null!");
        }

        if (rep.getPublicKey() != null && rep.getCertificate() != null) {
            throw new IllegalStateException("Both certificate and publicKey are not null!");
        }

        setOrRemoveAttr(client, privateKeyAttribute, rep.getPrivateKey());
        setOrRemoveAttr(client, publicKeyAttribute, rep.getPublicKey());
        setOrRemoveAttr(client, certificateAttribute, rep.getCertificate());
        setOrRemoveAttr(client, kidAttribute, rep.getKid());
    }

    public static CertificateRepresentation getCertificateFromRequest(KeycloakSession session) throws IOException {
        CertificateRepresentation info = new CertificateRepresentation();
        MultivaluedMap<String, FormPartValue> uploadForm = session.getContext().getHttpRequest().getMultiPartFormParameters();
        FormPartValue keystoreFormatPart = uploadForm.getFirst("keystoreFormat");
        if (keystoreFormatPart == null) {
            throw new BadRequestException("keystoreFormat cannot be null");
        }
        String keystoreFormat = keystoreFormatPart.asString();
        FormPartValue inputParts = uploadForm.getFirst("file");

        boolean fileEmpty = false;
        try {
            fileEmpty = inputParts == null || Strings.isEmpty(inputParts.asString());
        } catch (Exception e) {
            // ignore
        }

        if (fileEmpty) {
            throw new BadRequestException("file cannot be empty");
        }

        if (keystoreFormat.equals(CERTIFICATE_PEM)) {
            String pem = StreamUtil.readString(inputParts.asInputStream(), StandardCharsets.UTF_8);
            pem = PemUtils.removeBeginEnd(pem);

            // Validate format
            KeycloakModelUtils.getCertificate(pem);
            info.setCertificate(pem);
            return info;
        } else if (keystoreFormat.equals(PUBLIC_KEY_PEM)) {
            String pem = StreamUtil.readString(inputParts.asInputStream(), StandardCharsets.UTF_8);

            // Validate format
            KeycloakModelUtils.getPublicKey(pem);
            info.setPublicKey(pem);
            return info;
        } else if (keystoreFormat.equals(JSON_WEB_KEY_SET)) {
            String jwks = StreamUtil.readString(inputParts.asInputStream(), StandardCharsets.UTF_8);

            info = CertificateInfoHelper.jwksStringToSigCertificateRepresentation(jwks);
            return info;
        }

        String keyAlias = uploadForm.getFirst("keyAlias").asString();
        FormPartValue keyPasswordPart = uploadForm.getFirst("keyPassword");
        char[] keyPassword = keyPasswordPart != null ? keyPasswordPart.asString().toCharArray() : null;

        FormPartValue storePasswordPart = uploadForm.getFirst("storePassword");
        char[] storePassword = storePasswordPart != null ? storePasswordPart.asString().toCharArray() : null;
        PrivateKey privateKey = null;
        X509Certificate certificate = null;
        try {
            KeyStore keyStore = CryptoIntegration.getProvider().getKeyStore(KeystoreUtil.KeystoreFormat.valueOf(keystoreFormat));
            keyStore.load(inputParts.asInputStream(), storePassword);
            try {
                privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword);
            } catch (Exception e) {
                // ignore
            }
            certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
        } catch (Exception e) {
            logger.error("Error loading keystore", e);
            if (e.getCause() instanceof UnrecoverableKeyException keyException) {
                throw new BadRequestException(keyException.getMessage());
            } else {
                throw new BadRequestException("error loading keystore");
            }
        }

        if (privateKey != null) {
            String privateKeyPem = KeycloakModelUtils.getPemFromKey(privateKey);
            info.setPrivateKey(privateKeyPem);
        }

        if (certificate != null) {
            String certPem = KeycloakModelUtils.getPemFromCertificate(certificate);
            info.setCertificate(certPem);
        }

        return info;
    }

    private static void setOrRemoveAttr(ClientRepresentation client, String attrName, String attrValue) {
        if (attrValue != null) {
            if (client.getAttributes() == null) {
                client.setAttributes(new HashMap<>());
            }
            client.getAttributes().put(attrName, attrValue);
        } else {
            if (client.getAttributes() != null) {
                client.getAttributes().remove(attrName);
            }
        }
    }
}
