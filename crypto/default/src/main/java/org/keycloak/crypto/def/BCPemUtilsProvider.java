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

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.keycloak.common.util.PemException;
import org.keycloak.common.crypto.PemUtilsProvider;

import java.io.StringWriter;

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

}
