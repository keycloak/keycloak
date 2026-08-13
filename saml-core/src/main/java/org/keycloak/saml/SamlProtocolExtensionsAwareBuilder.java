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

package org.keycloak.saml;

import javax.xml.stream.XMLStreamWriter;

import org.keycloak.saml.common.exceptions.ProcessingException;

/**
 * Implementations of this interface are builders that can register &lt;samlp:Extensions&gt;
 * content providers.
 *
 * @author hmlnarik
 */
public interface SamlProtocolExtensionsAwareBuilder<T> {

    public interface NodeGenerator {
        /**
         * Generate contents of the &lt;samlp:Extensions&gt; tag. When this method is invoked,
         * the writer has already emitted the &lt;samlp:Extensions&gt; start tag.
         *
         * @param writer Writer to use for producing XML output
         * @throws ProcessingException If any exception fails
         */
        void write(XMLStreamWriter writer) throws ProcessingException;
    }

    /**
     * Adds a given node subtree as a SAML protocol extension into the SAML protocol message.
     *
     * @param extension
     * @return
     */
    T addExtension(NodeGenerator extension);
}
