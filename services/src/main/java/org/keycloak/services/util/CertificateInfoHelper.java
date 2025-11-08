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
import java.security.PublicKey;
import java.util.HashMap;

import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.models.ClientModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CertificateInfoHelper {


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

            PublicKey publicKey = JWKParser.create(publicKeyJwk).toPublicKey();
            String publicKeyPem = KeycloakModelUtils.getPemFromKey(publicKey);
            CertificateRepresentation info = new CertificateRepresentation();
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
