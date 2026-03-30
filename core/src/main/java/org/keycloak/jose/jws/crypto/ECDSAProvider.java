/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jose.jws.crypto;


import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.keycloak.common.util.PemUtils;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;

/**
 * @author <a href="mailto:tdiesler@proton.me">Thomas Diesler</a>
 */
public class ECDSAProvider implements SignatureProvider {

    public static String getJavaAlgorithm(Algorithm alg) {
        switch (alg) {
            case ES256:
                return "SHA256withECDSA";
            case ES384:
                return "SHA384withECDSA";
            case ES512:
                return "SHA512withECDSA";
            default:
                throw new IllegalArgumentException("Not a supported ECDSA Algorithm: " + alg);
        }
    }

    public static Signature getSignature(Algorithm alg) {
        try {
            return Signature.getInstance(getJavaAlgorithm(alg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyViaCertificate(JWSInput input, String cert) {
        X509Certificate certificate;
        try {
            certificate = PemUtils.decodeCertificate(cert);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return verify(input, certificate.getPublicKey());
    }

    public static boolean verify(JWSInput input, PublicKey publicKey) {
        try {
            Signature verifier = getSignature(input.getHeader().getAlgorithm());
            verifier.initVerify(publicKey);
            verifier.update(input.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8));
            byte[] derSignature = transcodeSignatureToDER(input.getSignature());
            return verifier.verify(derSignature);
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public boolean verify(JWSInput input, String key) {
        return verifyViaCertificate(input, key);
    }

    private static byte[] transcodeSignatureToDER(byte[] jwsSignature) {
        if (jwsSignature.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid ECDSA signature format");
        }

        int len = jwsSignature.length / 2;

        byte[] r = Arrays.copyOfRange(jwsSignature, 0, len);
        byte[] s = Arrays.copyOfRange(jwsSignature, len, jwsSignature.length);

        byte[] derR = derEncodeInteger(r);
        byte[] derS = derEncodeInteger(s);

        int totalLength = derR.length + derS.length;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(0x30); // SEQUENCE
        writeLength(out, totalLength);
        out.write(derR, 0, derR.length);
        out.write(derS, 0, derS.length);

        return out.toByteArray();
    }
    private static byte[] derEncodeInteger(byte[] value) {
        // remove leading zeros
        int offset = 0;
        while (offset < value.length - 1 && value[offset] == 0) {
            offset++;
        }

        int length = value.length - offset;

        // if highest bit is set, prepend 0x00
        boolean needsPadding = (value[offset] & 0x80) != 0;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(0x02); // INTEGER

        int contentLength = length + (needsPadding ? 1 : 0);
        writeLength(out, contentLength);

        if (needsPadding) {
            out.write(0x00);
        }

        out.write(value, offset, length);

        return out.toByteArray();
    }

    // In DER (Distinguished Encoding Rules), every element is encoded as TAG | LENGTH | VALUE
    // This method writes the LENGTH part
    private static void writeLength(ByteArrayOutputStream out, int length) {
        if (length < 128) {
            out.write(length);
        } else {
            int temp = length;
            int numBytes = 0;

            byte[] buffer = new byte[4]; // enough for int

            while (temp > 0) {
                buffer[buffer.length - 1 - numBytes] = (byte) (temp & 0xFF);
                temp >>= 8;
                numBytes++;
            }

            out.write(0x80 | numBytes);

            for (int i = buffer.length - numBytes; i < buffer.length; i++) {
                out.write(buffer[i]);
            }
        }
    }
}
