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
package org.keycloak.saml.processing.api.util;

import java.security.cert.X509Certificate;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.X509Data;

/**
 * Tools for {@link KeyInfo} object manipulation.
 * @author hmlnarik
 */
public class KeyInfoTools {

    /**
     * Returns the first object of the given class from the given Iterable.
     * @param <T>
     * @param objects
     * @param clazz
     * @return The object or {@code null} if not found.
     */
    public static <T> T getContent(Iterable<?> objects, Class<T> clazz) {
        if (objects == null) {
            return null;
        }
        for (Object o : objects) {
            if (clazz.isInstance(o)) {
                return (T) o;
            }
        }
        return null;
    }


    public static KeyName getKeyName(KeyInfo keyInfo) {
        return keyInfo == null ? null : getContent(keyInfo.getContent(), KeyName.class);
    }

    public static X509Data getX509Data(KeyInfo keyInfo) {
        return keyInfo == null ? null : getContent(keyInfo.getContent(), X509Data.class);
    }

    public static X509Certificate getX509Certificate(KeyInfo keyInfo) {
        X509Data d = getX509Data(keyInfo);
        return d == null ? null : getContent(d.getContent(), X509Certificate.class);
    }

}
