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
package org.keycloak.keys;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.RealmModel;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Optional;

public class GeneratedEcdsaKeyProvider extends AbstractEcKeyProvider {
    private static final Logger logger = Logger.getLogger(GeneratedEcdsaKeyProvider.class);

    public GeneratedEcdsaKeyProvider(RealmModel realm, ComponentModel model) {
        super(realm, model);
    }

    @Override
    protected KeyWrapper loadKey(RealmModel realm, ComponentModel model) {
        String privateEcdsaKeyBase64Encoded = model.getConfig().getFirst(GeneratedEcdsaKeyProviderFactory.ECDSA_PRIVATE_KEY_KEY);
        String publicEcdsaKeyBase64Encoded = model.getConfig().getFirst(GeneratedEcdsaKeyProviderFactory.ECDSA_PUBLIC_KEY_KEY);
        String ecInNistRep = model.getConfig().getFirst(GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY);
        boolean generateCertificate = Optional.ofNullable(model.getConfig()
                                                               .getFirst(Attributes.EC_GENERATE_CERTIFICATE_KEY))
                                                               .map(Boolean::parseBoolean)
                                                               .orElse(false);

        try {
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateEcdsaKeyBase64Encoded));
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey decodedPrivateKey = kf.generatePrivate(privateKeySpec);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(publicEcdsaKeyBase64Encoded));
            PublicKey decodedPublicKey = kf.generatePublic(publicKeySpec);

            KeyPair keyPair = new KeyPair(decodedPublicKey, decodedPrivateKey);
            X509Certificate selfSignedCertificate = Optional.ofNullable(model.getConfig()
                                                                             .getFirst(Attributes.CERTIFICATE_KEY))
                                                            .map(PemUtils::decodeCertificate)
                                                            .orElse(null);
            if (generateCertificate && selfSignedCertificate == null)
            {
                selfSignedCertificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, realm.getName());
                model.getConfig().put(Attributes.CERTIFICATE_KEY,
                                      List.of(Base64.encodeBytes(selfSignedCertificate.getEncoded())));
            }

            return createKeyWrapper(keyPair,
                                    GeneratedEcdsaKeyProviderFactory.convertECDomainParmNistRepToJWSAlgorithm(
                                            ecInNistRep), KeyUse.SIG,
                                    selfSignedCertificate);
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            logger.warnf("Exception at decodeEcdsaPublicKey. %s", e.toString());
            return null;
        }

    }

}
