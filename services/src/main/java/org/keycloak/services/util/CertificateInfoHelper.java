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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.authenticators.client.ClientAuthUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CertificateInfoHelper {


    public static final String PRIVATE_KEY = "private.key";
    public static final String X509CERTIFICATE = "certificate";
    public static final String PUBLIC_KEY = "public.key";


    // CLIENT MODEL METHODS

    public static CertificateRepresentation getCertificateFromClient(ClientModel client, String attributePrefix) {
        String privateKeyAttribute = attributePrefix + "." + PRIVATE_KEY;
        String certificateAttribute = attributePrefix + "." + X509CERTIFICATE;
        String publicKeyAttribute = attributePrefix + "." + PUBLIC_KEY;

        CertificateRepresentation rep = new CertificateRepresentation();
        rep.setCertificate(client.getAttribute(certificateAttribute));
        rep.setPublicKey(client.getAttribute(publicKeyAttribute));
        rep.setPrivateKey(client.getAttribute(privateKeyAttribute));

        return rep;
    }


    public static void updateClientModelCertificateInfo(ClientModel client, CertificateRepresentation rep, String attributePrefix) {
        String privateKeyAttribute = attributePrefix + "." + PRIVATE_KEY;
        String certificateAttribute = attributePrefix + "." + X509CERTIFICATE;
        String publicKeyAttribute = attributePrefix + "." + PUBLIC_KEY;

        if (rep.getPublicKey() == null && rep.getCertificate() == null) {
            throw new IllegalStateException("Both certificate and publicKey are null!");
        }

        if (rep.getPublicKey() != null && rep.getCertificate() != null) {
            throw new IllegalStateException("Both certificate and publicKey are not null!");
        }

        setOrRemoveAttr(client, privateKeyAttribute, rep.getPrivateKey());
        setOrRemoveAttr(client, publicKeyAttribute, rep.getPublicKey());
        setOrRemoveAttr(client, certificateAttribute, rep.getCertificate());
    }

    private static void setOrRemoveAttr(ClientModel client, String attrName, String attrValue) {
        if (attrValue != null) {
            client.setAttribute(attrName, attrValue);
        } else {
            client.removeAttribute(attrName);
        }
    }


    public static PublicKey getSignatureValidationKey(ClientModel client, String attributePrefix) throws ModelException {
        CertificateRepresentation certInfo = getCertificateFromClient(client, attributePrefix);

        String encodedCertificate = certInfo.getCertificate();
        String encodedPublicKey = certInfo.getPublicKey();

        if (encodedCertificate == null && encodedPublicKey == null) {
            throw new ModelException("Client doesn't have certificate or publicKey configured");
        }

        if (encodedCertificate != null && encodedPublicKey != null) {
            throw new ModelException("Client has both publicKey and certificate configured");
        }

        // TODO: Caching of publicKeys / certificates, so it doesn't need to be always computed from pem. For performance reasons...
        if (encodedCertificate != null) {
            X509Certificate clientCert = KeycloakModelUtils.getCertificate(encodedCertificate);
            return clientCert.getPublicKey();
        } else {
            return KeycloakModelUtils.getPublicKey(encodedPublicKey);
        }
    }


    // CLIENT REPRESENTATION METHODS

    public static void updateClientRepresentationCertificateInfo(ClientRepresentation client, CertificateRepresentation rep, String attributePrefix) {
        String privateKeyAttribute = attributePrefix + "." + PRIVATE_KEY;
        String certificateAttribute = attributePrefix + "." + X509CERTIFICATE;
        String publicKeyAttribute = attributePrefix + "." + PUBLIC_KEY;

        if (rep.getPublicKey() == null && rep.getCertificate() == null) {
            throw new IllegalStateException("Both certificate and publicKey are null!");
        }

        if (rep.getPublicKey() != null && rep.getCertificate() != null) {
            throw new IllegalStateException("Both certificate and publicKey are not null!");
        }

        setOrRemoveAttr(client, privateKeyAttribute, rep.getPrivateKey());
        setOrRemoveAttr(client, publicKeyAttribute, rep.getPublicKey());
        setOrRemoveAttr(client, certificateAttribute, rep.getCertificate());
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
