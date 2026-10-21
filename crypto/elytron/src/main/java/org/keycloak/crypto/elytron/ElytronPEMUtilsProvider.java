/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crypto.elytron;

import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;

import org.keycloak.common.crypto.PemUtilsProvider;
import org.keycloak.common.util.DerUtils;
import org.keycloak.common.util.PemException;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:david.anderson@redhat.com">David Anderson</a>
 */
public class ElytronPEMUtilsProvider extends PemUtilsProvider {

    Logger log = Logger.getLogger(ElytronPEMUtilsProvider.class);

    @Override
    protected String encode(Object obj) {
        String encoded = null;
        if(obj instanceof Key ) {
            byte[] b = ((Key)obj).getEncoded();
            encoded = Base64.getEncoder().encodeToString(b);
        } else if(obj instanceof Certificate) {
            byte[] c;
            try {
                c = ((Certificate)obj).getEncoded();
                encoded = Base64.getEncoder().encodeToString(c);
            } catch (CertificateEncodingException e) {
                log.warn("Failed to encoded certificate.", e);
                throw new RuntimeException(e);
            }
        }
        return encoded;
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
