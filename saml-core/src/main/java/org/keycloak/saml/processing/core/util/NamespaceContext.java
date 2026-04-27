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

package org.keycloak.saml.processing.core.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Helper class in process of parsing signature out of SAML token.
 * usage example:
 * <code>
 * xpath.setNamespaceContext(
 * NamespaceContext.create()
 * .addNsUriPair(xmlSignatureNSPrefix, JBossSAMLURIConstants.XMLDSIG_NSURI.get())
 * );
 * </code>
 *
 * @author Peter Skopek: pskopek at redhat dot com
 */

public class NamespaceContext implements javax.xml.namespace.NamespaceContext {

    private Map<String, String> nsMap = new HashMap<>();

    public NamespaceContext() {
    }

    public NamespaceContext(String prefix, String uri) {
        nsMap.put(prefix, uri);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.xml.namespace.NamespaceContext#getNamespaceURI(java.lang.String)
     */
    public String getNamespaceURI(String prefix) {
        return nsMap.get(prefix);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.namespace.NamespaceContext#getPrefix(java.lang.String)
     */
    public String getPrefix(String namespaceURI) {
        for (var entry : nsMap.entrySet()) {
            String value = entry.getValue();
            if (value.equals(namespaceURI)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.namespace.NamespaceContext#getPrefixes(java.lang.String)
     */
    public Iterator<String> getPrefixes(String namespaceURI) {
        return nsMap.keySet().iterator();
    }

    public NamespaceContext addNsUriPair(String ns, String uri) {
        nsMap.put(ns, uri);
        return this;
    }

    /**
     * Create new NamespaceContext for use.
     *
     * @return
     */
    public static NamespaceContext create() {
        return new NamespaceContext();
    }
}
