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

package org.keycloak.crypto.def;

import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.keycloak.common.crypto.PemUtilsProvider;
import org.keycloak.common.util.DerUtils;
import org.keycloak.common.util.PemException;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

/**
 * Encodes Key or Certificates to PEM format string
 *
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 * @version $Revision: 1 $
 */
public class BCPemUtilsProvider extends PemUtilsProvider {


    /**
     * Encode object to JCA PEM String using BC libraries
     * 
     * @param obj
     * @return The encoded PEM string
     */
    @Override
    protected String encode(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            pemWriter.writeObject(obj);
            pemWriter.flush();
            pemWriter.close();
            String s = writer.toString();
            return removeBeginEnd(s);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

    @Override
    public PublicKey decodePublicKey(String pem) {
        try {
            // try to decode using SubjectPublicKeyInfo which allows to know the key type
            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(pemToDer(pem));
            if (publicKeyInfo != null && publicKeyInfo.getAlgorithm() != null) {
                return new JcaPEMKeyConverter().getPublicKey(publicKeyInfo);
            }
        } catch (Exception e) {
            // error reading PEM object just go to previous RSA forced key
        }

        // assume RSA if it cannot be decoded from BC knowing the key
        return decodePublicKey(pem, "RSA");
    }

    @Override
    public PrivateKey decodePrivateKey(String pem) {
        if (pem == null) {
            return null;
        }

        try {
            byte[] der = pemToDer(pem);
            return DerUtils.decodePrivateKey(der);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

}
