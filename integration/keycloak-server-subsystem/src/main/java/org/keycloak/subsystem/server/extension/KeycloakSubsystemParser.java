/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.subsystem.server.extension;

import org.keycloak.subsystem.server.extension.authserver.AuthServerDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.List;

/**
 * The subsystem parser, which uses stax to read and write to and from xml
 */
class KeycloakSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        // Require no attributes
        ParseUtils.requireNoAttributes(reader);
        ModelNode addKeycloakSub = Util.createAddOperation(PathAddress.pathAddress(KeycloakExtension.PATH_SUBSYSTEM));
        list.add(addKeycloakSub);

        while (reader.hasNext() && nextTag(reader) != END_ELEMENT) {
            if (reader.getLocalName().equals(AuthServerDefinition.TAG_NAME)) {
                readAuthServer(reader, list);
            }
        }
    }

    // used for debugging
    private int nextTag(XMLExtendedStreamReader reader) throws XMLStreamException {
        return reader.nextTag();
    }

    private void readAuthServer(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        String authServerName = readNameAttribute(reader);
        ModelNode addAuthServer = new ModelNode();
        addAuthServer.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, KeycloakExtension.SUBSYSTEM_NAME),
                                                   PathElement.pathElement(AuthServerDefinition.TAG_NAME, authServerName));
        addAuthServer.get(ModelDescriptionConstants.OP_ADDR).set(addr.toModelNode());

        while (reader.hasNext() && nextTag(reader) != END_ELEMENT) {
            String tagName = reader.getLocalName();
            SimpleAttributeDefinition def = AuthServerDefinition.lookup(tagName);
            if (def == null) throw new XMLStreamException("Unknown auth-server tag " + tagName);
            def.parseAndSetParameter(reader.getElementText(), addAuthServer, reader);
        }

        list.add(addAuthServer);
    }

    // expects that the current tag will have one single attribute called "name"
    private String readNameAttribute(XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attr = reader.getAttributeLocalName(i);
            if (attr.equals("name")) {
                name = reader.getAttributeValue(i);
                continue;
            }
            throw ParseUtils.unexpectedAttribute(reader, i);
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(KeycloakExtension.NAMESPACE, false);
        writeAuthServers(writer, context);
        writer.writeEndElement();
    }

    private void writeAuthServers(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        if (!context.getModelNode().get(AuthServerDefinition.TAG_NAME).isDefined()) {
            return;
        }
        for (Property authServer : context.getModelNode().get(AuthServerDefinition.TAG_NAME).asPropertyList()) {
            writer.writeStartElement(AuthServerDefinition.TAG_NAME);
            writer.writeAttribute("name", authServer.getName());
            ModelNode authServerElements = authServer.getValue();
            for (AttributeDefinition element : AuthServerDefinition.ALL_ATTRIBUTES) {
                element.marshallAsElement(authServerElements, writer);
            }

            writer.writeEndElement();
        }
    }

    // code taken from org.jboss.as.controller.AttributeMarshaller
    private void writeCharacters(XMLExtendedStreamWriter writer, String content) throws XMLStreamException {
        if (content.indexOf('\n') > -1) {
            // Multiline content. Use the overloaded variant that staxmapper will format
            writer.writeCharacters(content);
        } else {
            // Staxmapper will just output the chars without adding newlines if this is used
            char[] chars = content.toCharArray();
            writer.writeCharacters(chars, 0, chars.length);
        }
    }

}
