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

package org.keycloak.crypto.fips;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.keycloak.common.crypto.PemUtilsProvider;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

/**
 * Encodes Key or Certificates to PEM format string
 *
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 * @version $Revision: 1 $
 */
public class BCFIPSPemUtilsProvider extends PemUtilsProvider {


    /**
     * Encode object to JCA PEM String using BC FIPS libraries
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
            boolean beginEndAvailable = pem.startsWith("-----BEGIN");
            Object parsedPk;
            if (beginEndAvailable) { // No fallback needed as BC should know the format of the key (based on the phrase like BEGIN PRIVATE KEY, BEGIN RSA PRIVATE KEY, BEGIN EC PRIVATE KEY etc)
                parsedPk = readPrivateKeyObject(pem);
            } else {
                try {
                    // Case for the PEM in traditional format
                    String rsaPem = PemUtils.addRsaPrivateKeyBeginEnd(pem);
                    parsedPk = readPrivateKeyObject(rsaPem);
                } catch (IOException ioe) {
                    // Case for generic PKCS#8 represented keys
                    pem = PemUtils.addPrivateKeyBeginEnd(pem);
                    parsedPk = readPrivateKeyObject(pem);
                }
            }

            PrivateKeyInfo privateKeyInfo;
            if (parsedPk instanceof PEMKeyPair) {
                // Usually for keys of known format (For example when PEM starts with "BEGIN RSA PRIVATE KEY")
                PEMKeyPair pemKeyPair = (PEMKeyPair)parsedPk;
                privateKeyInfo = pemKeyPair.getPrivateKeyInfo();
            } else if (parsedPk instanceof PrivateKeyInfo) {
                // Usually for PKCS#8 formatted keys of unknown type ("BEGIN PRIVATE KEY")
                privateKeyInfo = (PrivateKeyInfo) parsedPk;
            } else {
                throw new IllegalStateException("Unknown type returned by PEMParser when parsing private key: " + parsedPk.getClass());
            }

            return new JcaPEMKeyConverter()
                    .setProvider(BouncyIntegration.PROVIDER)
                    .getPrivateKey(privateKeyInfo);
        } catch (Exception e) {
            throw new PemException(e);
        }
    }

    private Object readPrivateKeyObject(String pemWithBeginEnd) throws IOException {
        PEMParser parser = new PEMParser(new StringReader(pemWithBeginEnd));
        return parser.readObject();
    }

}
