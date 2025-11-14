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

package org.keycloak.adapters.saml.config.parsers;

import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleMappingParser extends AbstractKeycloakSamlAdapterV1Parser<Set<String>> {

    private static final RoleMappingParser INSTANCE = new RoleMappingParser();

    private RoleMappingParser() {
        super(KeycloakSamlAdapterV1QNames.ROLE_IDENTIFIERS);
    }

    public static RoleMappingParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected Set<String> instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new HashSet<>();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, Set<String> target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE:
                target.add(StaxParserUtil.getRequiredAttributeValueRP(elementDetail, KeycloakSamlAdapterV1QNames.ATTR_NAME));
                break;
        }
    }
}
