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
package org.keycloak.saml.common.util;

import java.security.cert.X509Certificate;

/**
 *
 * @author hmlnarik
 */
public enum XmlKeyInfoKeyNameTransformer {
    NONE            { @Override public String getKeyName(String keyId, X509Certificate certificate) { return null; } },
    KEY_ID          { @Override public String getKeyName(String keyId, X509Certificate certificate) { return keyId; } },
    CERT_SUBJECT    { @Override public String getKeyName(String keyId, X509Certificate certificate) {
                        return certificate == null
                               ? null
                               : (certificate.getSubjectDN() == null
                                  ? null
                                  : certificate.getSubjectDN().getName());
                    } }
    ;

    public abstract String getKeyName(String keyId, X509Certificate certificate);

    public static XmlKeyInfoKeyNameTransformer from(String name, XmlKeyInfoKeyNameTransformer defaultValue) {
        if (name == null) {
            return defaultValue;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
