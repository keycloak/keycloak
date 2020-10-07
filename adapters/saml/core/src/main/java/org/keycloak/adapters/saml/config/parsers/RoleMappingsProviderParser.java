/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import java.util.Properties;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.adapters.saml.config.SP;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * A parser for the {@code <RoleMappingsProvider>} element., represented by the role-mappings-provider-type in the schema.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class RoleMappingsProviderParser extends AbstractKeycloakSamlAdapterV1Parser<SP.RoleMappingsProviderConfig> {

    private static final RoleMappingsProviderParser INSTANCE = new RoleMappingsProviderParser();

    private RoleMappingsProviderParser() {
        super(KeycloakSamlAdapterV1QNames.ROLE_MAPPINGS_PROVIDER);
    }

    public static RoleMappingsProviderParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected SP.RoleMappingsProviderConfig instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        SP.RoleMappingsProviderConfig providerConfig = new SP.RoleMappingsProviderConfig();
        providerConfig.setId(StaxParserUtil.getRequiredAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_ID));
        providerConfig.setConfiguration(new Properties());
        return providerConfig;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, SP.RoleMappingsProviderConfig target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
        switch(element) {
            case PROPERTY:
                final String name = StaxParserUtil.getRequiredAttributeValueRP(elementDetail, KeycloakSamlAdapterV1QNames.ATTR_NAME);
                final String value = StaxParserUtil.getRequiredAttributeValueRP(elementDetail, KeycloakSamlAdapterV1QNames.ATTR_VALUE);
                target.addConfigurationProperty(name, value);
                break;
        }
    }
}
