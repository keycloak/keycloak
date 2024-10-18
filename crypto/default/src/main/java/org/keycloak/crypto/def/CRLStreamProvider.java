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

import org.keycloak.common.crypto.CRLProvider;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509CRL;

/**
 * Crl Provider using the streaming parser
 *
 * @author Scott Tustison
 */
public class CRLStreamProvider implements CRLProvider {
    @Override
    public X509CRL generateCRL(File crlFile) throws IOException {
        return new StreamingCrlParser(crlFile);
    }
}
