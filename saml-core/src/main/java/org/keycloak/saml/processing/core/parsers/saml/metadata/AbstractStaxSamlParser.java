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
package org.keycloak.saml.processing.core.parsers.saml.metadata;

import org.keycloak.saml.common.constants.JBossSAMLConstants;
import javax.xml.namespace.QName;
import org.keycloak.saml.common.parsers.AbstractStaxParser;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractStaxSamlParser<T> extends AbstractStaxParser<T, JBossSAMLConstants> {

    public AbstractStaxSamlParser(JBossSAMLConstants expectedStartElement) {
        super(expectedStartElement.getAsQName(), JBossSAMLConstants.UNKNOWN_VALUE);
    }

    @Override
    protected boolean isUnknownElement(JBossSAMLConstants token) {
        return token == JBossSAMLConstants.UNKNOWN_VALUE;
    }

    @Override
    protected JBossSAMLConstants getElementFromName(QName name) {
        JBossSAMLConstants res = JBossSAMLConstants.from(name);

        if ((res == null || res == JBossSAMLConstants.UNKNOWN_VALUE) && name != null) {
            // Relax and search regardless of namespace
            res = JBossSAMLConstants.from(name.getLocalPart());
        }

        return res;
    }

}
