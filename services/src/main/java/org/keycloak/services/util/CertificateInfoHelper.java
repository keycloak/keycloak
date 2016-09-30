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

import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;

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

        CertificateRepresentation rep = new CertificateRepresentation();
        rep.setCertificate(client.getAttribute(certificateAttribute));
        rep.setPublicKey(client.getAttribute(publicKeyAttribute));
        rep.setPrivateKey(client.getAttribute(privateKeyAttribute));
        rep.setKid(client.getAttribute(kidAttribute));

        return rep;
    }


    public static void updateClientModelCertificateInfo(ClientModel client, CertificateRepresentation rep, String attributePrefix) {
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
