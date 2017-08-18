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

package org.keycloak.subsystem.saml.as7.xml;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * An XML stream writer which nicely formats the XML for configuration files.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class FormattingXMLStreamWriter implements XMLExtendedStreamWriter, XMLStreamConstants {
    private static final String NO_NAMESPACE = new String();
    private final XMLStreamWriter delegate;
    private final ArrayDeque<ArgRunnable> attrQueue = new ArrayDeque<ArgRunnable>();
    private int level;
    private int state = START_DOCUMENT;
    private boolean indentEndElement = false;
    private ArrayDeque<String> unspecifiedNamespaces = new ArrayDeque<String>();


    public FormattingXMLStreamWriter(final XMLStreamWriter delegate) {
        this.delegate = delegate;
        unspecifiedNamespaces.push(NO_NAMESPACE);
    }

    private void nl() throws XMLStreamException {
        delegate.writeCharacters("\n");
    }

    private void indent() throws XMLStreamException {
        int level = this.level;
        final XMLStreamWriter delegate = this.delegate;
        for (int i = 0; i < level; i ++) {
            delegate.writeCharacters("    ");
        }
    }

    private interface ArgRunnable {
        public void run(int arg) throws XMLStreamException;
    }

    @Override
    public void setUnspecifiedElementNamespace(final String namespace) {
        ArrayDeque<String> namespaces = this.unspecifiedNamespaces;
        namespaces.pop();
        namespaces.push(namespace == null ? NO_NAMESPACE : namespace);
    }

    private String nestUnspecifiedNamespace() {
        ArrayDeque<String> namespaces = unspecifiedNamespaces;
        String clone = namespaces.getFirst();
        namespaces.push(clone);
        return clone;
    }

    @Override
    public void writeStartElement(final String localName) throws XMLStreamException {
        ArrayDeque<String> namespaces = unspecifiedNamespaces;
        String namespace = namespaces.getFirst();
        if (namespace == null ? NO_NAMESPACE != null : ! namespace.equals(NO_NAMESPACE)) {
            writeStartElement(namespace, localName);
            return;
        }

        unspecifiedNamespaces.push(namespace);

        // If this is a nested element flush the outer
        runAttrQueue();
        nl();
        indent();
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                if (arg == 0) {
                    delegate.writeStartElement(localName);
                } else {
                    delegate.writeEmptyElement(localName);
                }
            }
        });

        level++;
        state = START_ELEMENT;
        indentEndElement = false;
    }

    @Override
    public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
        nestUnspecifiedNamespace();

        // If this is a nested element flush the outer
        runAttrQueue();
        nl();
        indent();
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                if (arg == 0) {
                    delegate.writeStartElement(namespaceURI, localName);
                } else {
                    delegate.writeEmptyElement(namespaceURI, localName);
                }
            }
        });
        level++;
        state = START_ELEMENT;
        indentEndElement = false;
    }

    @Override
    public void writeStartElement(final String prefix, final String localName, final String namespaceURI) throws XMLStreamException {
        nestUnspecifiedNamespace();

        // If this is a nested element flush the outer
        runAttrQueue();
        nl();
        indent();
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                if (arg == 0) {
                    delegate.writeStartElement(prefix, localName, namespaceURI);
                } else {
                    delegate.writeEmptyElement(prefix, localName, namespaceURI);
                }
            }
        });
        level++;
        state = START_ELEMENT;
        indentEndElement = false;
    }

    @Override
    public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
        runAttrQueue();
        nl();
        indent();
        delegate.writeEmptyElement(namespaceURI, localName);
        state = END_ELEMENT;
    }

    @Override
    public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI) throws XMLStreamException {
        runAttrQueue();
        nl();
        indent();
        delegate.writeEmptyElement(prefix, localName, namespaceURI);
        state = END_ELEMENT;
    }

    @Override
    public void writeEmptyElement(final String localName) throws XMLStreamException {
        String namespace = unspecifiedNamespaces.getFirst();
        if (namespace == null ? NO_NAMESPACE != null : ! namespace.equals(NO_NAMESPACE)) {
            writeEmptyElement(namespace, localName);
            return;
        }

        runAttrQueue();
        nl();
        indent();
        delegate.writeEmptyElement(localName);
        state = END_ELEMENT;
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        level--;
        if (state != START_ELEMENT) {
            runAttrQueue();
            if (state != CHARACTERS || indentEndElement) {
                nl();
                indent();
                indentEndElement = false;
            }
            delegate.writeEndElement();
        } else {
            // Change the start element to an empty element
            ArgRunnable start = attrQueue.poll();
            if (start == null) {
                delegate.writeEndElement();
            } else {
                start.run(1);
                // Write everything else
                runAttrQueue();
            }
        }

        unspecifiedNamespaces.pop();
        state = END_ELEMENT;
    }

    private void runAttrQueue() throws XMLStreamException {
        ArgRunnable attr;
        while ((attr = attrQueue.poll()) != null) {
            attr.run(0);
        }
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
        state = END_DOCUMENT;
    }

    @Override
    public void close() throws XMLStreamException {
        delegate.close();
        state = END_DOCUMENT;
    }

    @Override
    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    @Override
    public void writeAttribute(final String localName, final String value) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                try {
                    delegate.writeAttribute(localName, value);
                } catch (XMLStreamException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        });
    }

    @Override
    public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String value) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeAttribute(prefix, namespaceURI, localName, value);
            }
        });
    }

    @Override
    public void writeAttribute(final String namespaceURI, final String localName, final String value) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeAttribute(namespaceURI, localName, value);
            }
        });
    }

    @Override
    public void writeAttribute(final String localName, final String[] values) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeAttribute(localName, join(values));
            }
        });
    }

    @Override
    public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String[] values) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeAttribute(prefix, namespaceURI, localName, join(values));
            }
        });
    }

    @Override
    public void writeAttribute(final String namespaceURI, final String localName, final String[] values) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeAttribute(namespaceURI, localName, join(values));
            }
        });
    }

    @Override
    public void writeAttribute(final String localName, final Iterable<String> values) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeAttribute(localName, join(values));
            }
        });
    }

    @Override
    public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final Iterable<String> values) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeAttribute(prefix, namespaceURI, localName, join(values));
            }
        });
    }

    @Override
    public void writeAttribute(final String namespaceURI, final String localName, final Iterable<String> values) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeAttribute(namespaceURI, localName, join(values));
            }
        });
    }

    @Override
    public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeNamespace(prefix, namespaceURI);
            }
        });
    }

    @Override
    public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
        attrQueue.add(new ArgRunnable() {
            public void run(int arg) throws XMLStreamException {
                delegate.writeDefaultNamespace(namespaceURI);
            }
        });
    }

    @Override
    public void writeComment(final String data) throws XMLStreamException {
        runAttrQueue();
        nl();
        nl();
        indent();
        final StringBuilder b = new StringBuilder(data.length());
        final Iterator<String> i = Spliterator.over(data, '\n');
        if (! i.hasNext()) {
            return;
        } else {
            final String first = i.next();
            if (! i.hasNext()) {
                delegate.writeComment(" " + first + " ");
                state = COMMENT;
                return;
            } else {
                b.append('\n');
                for (int q = 0; q < level; q++) {
                    b.append("    ");
                }
                b.append("  ~ ");
                b.append(first);
                do {
                    b.append('\n');
                    for (int q = 0; q < level; q++) {
                        b.append("    ");
                    }
                    b.append("  ~ ");
                    b.append(i.next());
                } while (i.hasNext());
            }
            b.append('\n');
            for (int q = 0; q < level; q ++) {
                b.append("    ");
            }
            b.append("  ");
            delegate.writeComment(b.toString());
            state = COMMENT;
        }
    }

    @Override
    public void writeProcessingInstruction(final String target) throws XMLStreamException {
        runAttrQueue();
        nl();
        indent();
        delegate.writeProcessingInstruction(target);
        state = PROCESSING_INSTRUCTION;
    }

    @Override
    public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
        runAttrQueue();
        nl();
        indent();
        delegate.writeProcessingInstruction(target, data);
        state = PROCESSING_INSTRUCTION;
    }

    @Override
    public void writeCData(final String data) throws XMLStreamException {
        runAttrQueue();
        delegate.writeCData(data);
        state = CDATA;
    }

    @Override
    public void writeDTD(final String dtd) throws XMLStreamException {
        nl();
        indent();
        delegate.writeDTD(dtd);
        state = DTD;
    }

    @Override
    public void writeEntityRef(final String name) throws XMLStreamException {
        runAttrQueue();
        delegate.writeEntityRef(name);
        state = ENTITY_REFERENCE;
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
        nl();
        state = START_DOCUMENT;
    }

    @Override
    public void writeStartDocument(final String version) throws XMLStreamException {
        delegate.writeStartDocument(version);
        nl();
        state = START_DOCUMENT;
    }

    @Override
    public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
        delegate.writeStartDocument(encoding, version);
        nl();
        state = START_DOCUMENT;
    }

    @Override
    public void writeCharacters(final String text) throws XMLStreamException {
        runAttrQueue();
        if (state != CHARACTERS) {
            nl();
            indent();
        }
        final Iterator<String> iterator = Spliterator.over(text, '\n');
        while (iterator.hasNext()) {
            final String t = iterator.next();
            delegate.writeCharacters(t);
            if (iterator.hasNext()) {
                nl();
                indent();
            }
        }
        state = CHARACTERS;
        indentEndElement = true;
    }

    @Override
    public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
        runAttrQueue();
        delegate.writeCharacters(text, start, len);
        state = CHARACTERS;
    }

    @Override
    public String getPrefix(final String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    @Override
    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        delegate.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        runAttrQueue();
        delegate.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
        delegate.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public Object getProperty(final String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

    private static String join(final String[] values) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            final String s = values[i];
            if (s != null) {
                if (i > 0) {
                    b.append(' ');
                }
                b.append(s);
            }
        }
        return b.toString();
    }

    private static String join(final Iterable<String> values) {
        final StringBuilder b = new StringBuilder();
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            final String s = iterator.next();
            if (s != null) {
                b.append(s);
                if (iterator.hasNext()) b.append(' ');
            }
        }
        return b.toString();
    }
}
