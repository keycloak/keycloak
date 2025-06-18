/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.processing.core.parsers.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 *
 * @author hmlnarik
 */
public class QNameEnumLookup<E extends Enum<E> & HasQName> {

    private final Map<QName, E> qNameConstants;

    public QNameEnumLookup(E[] e) {
        Map<QName, E> q = new HashMap<>(e.length);
        E old;
        for (E c : e) {
            QName qName = c.getQName();
            if ((old = q.put(qName, c)) != null) {
                throw new IllegalStateException("Same name " + qName + " used for two distinct constants: " + c + ", " + old);
            }

            // Add the relaxed version without namespace
            if (qName.getNamespaceURI() != null && ! Objects.equals(qName.getNamespaceURI(), XMLConstants.NULL_NS_URI)) {
                qName = new QName(qName.getLocalPart());
                if (q.containsKey(qName)) {
                    q.put(qName, null);
                } else {
                    q.put(qName, c);
                }
            }
        }
        this.qNameConstants = Collections.unmodifiableMap(q);
    }

    /**
     * Looks up the given {@code name} and returns the corresponding constant.
     * @param name
     * @return
     */
    public E from(QName name) {
        E c = qNameConstants.get(name);
        if (c == null) {
            name = new QName(name.getLocalPart());
            c = qNameConstants.get(name);
        }
        return c;
    }
}
