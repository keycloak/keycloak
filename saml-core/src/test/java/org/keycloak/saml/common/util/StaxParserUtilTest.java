/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.saml.common.exceptions.ParsingException;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author hmlnarik
 */
public class StaxParserUtilTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private void assertStartTag(XMLEvent event, String tagName) {
        assertThat(event, instanceOf(StartElement.class));
        assertThat(((StartElement) event).getName().getLocalPart(), is(tagName));
    }

    private void assertEndTag(XMLEvent event, String tagName) {
        assertThat(event, instanceOf(EndElement.class));
        assertThat(((EndElement) event).getName().getLocalPart(), is(tagName));
    }

    private void assertCharacters(XMLEvent event, Matcher<String> matcher) {
        assertThat(event, instanceOf(Characters.class));
        assertThat(((Characters) event).getData(), matcher);
    }

    @Test
    public void testBypassElementBlock() throws XMLStreamException, ParsingException {
        String xml = "<a><b><c>test</c>"
          + "<d>aa</d></b></a>";
        XMLEventReader reader = StaxParserUtil.getXMLEventReader(IOUtils.toInputStream(xml, Charset.defaultCharset()));

        assertThat(reader.nextEvent(), instanceOf(StartDocument.class));

        assertStartTag(reader.nextEvent(), "a");
        assertStartTag(reader.nextEvent(), "b");
        assertStartTag(reader.nextEvent(), "c");
        assertCharacters(reader.nextEvent(), is("test"));
        assertEndTag(reader.nextEvent(), "c");

        StaxParserUtil.bypassElementBlock(reader, "d");

        assertEndTag(reader.nextEvent(), "b");
        assertEndTag(reader.nextEvent(), "a");
    }

    @Test
    public void testBypassElementBlockAnon() throws XMLStreamException, ParsingException {
        String xml = "<a><b><c>test</c>"
          + "<d>aa</d></b></a>";
        XMLEventReader reader = StaxParserUtil.getXMLEventReader(IOUtils.toInputStream(xml, Charset.defaultCharset()));

        assertThat(reader.nextEvent(), instanceOf(StartDocument.class));

        assertStartTag(reader.nextEvent(), "a");
        assertStartTag(reader.nextEvent(), "b");
        assertStartTag(reader.nextEvent(), "c");
        assertCharacters(reader.nextEvent(), is("test"));
        assertEndTag(reader.nextEvent(), "c");

        StaxParserUtil.bypassElementBlock(reader);

        assertEndTag(reader.nextEvent(), "b");
        assertEndTag(reader.nextEvent(), "a");
    }

    @Test
    public void testBypassElementBlockNested() throws XMLStreamException, ParsingException {
        String xml = "<a><b><c>test</c>"
          + "<d>aa<d>nestedD</d></d></b></a>";
        XMLEventReader reader = StaxParserUtil.getXMLEventReader(IOUtils.toInputStream(xml, Charset.defaultCharset()));

        assertThat(reader.nextEvent(), instanceOf(StartDocument.class));

        assertStartTag(reader.nextEvent(), "a");
        assertStartTag(reader.nextEvent(), "b");
        assertStartTag(reader.nextEvent(), "c");
        assertCharacters(reader.nextEvent(), is("test"));
        assertEndTag(reader.nextEvent(), "c");

        StaxParserUtil.bypassElementBlock(reader, "d");

        assertEndTag(reader.nextEvent(), "b");
        assertEndTag(reader.nextEvent(), "a");
    }

    @Test
    public void testBypassElementBlockNestedAnon() throws XMLStreamException, ParsingException {
        String xml = "<a><b><c>test</c>"
          + "<d>aa<d>nestedD</d></d></b></a>";
        XMLEventReader reader = StaxParserUtil.getXMLEventReader(IOUtils.toInputStream(xml, Charset.defaultCharset()));

        assertThat(reader.nextEvent(), instanceOf(StartDocument.class));

        assertStartTag(reader.nextEvent(), "a");
        assertStartTag(reader.nextEvent(), "b");
        assertStartTag(reader.nextEvent(), "c");
        assertCharacters(reader.nextEvent(), is("test"));
        assertEndTag(reader.nextEvent(), "c");

        StaxParserUtil.bypassElementBlock(reader);

        assertEndTag(reader.nextEvent(), "b");
        assertEndTag(reader.nextEvent(), "a");
    }

    @Test
    public void testBypassElementBlockWrongPairing() throws XMLStreamException, ParsingException {
        String xml = "<a><b><c>test</c>"
          + "<d><b>aa</d><d>nestedD</d></d></b></a>";
        XMLEventReader reader = StaxParserUtil.getXMLEventReader(IOUtils.toInputStream(xml, Charset.defaultCharset()));

        assertThat(reader.nextEvent(), instanceOf(StartDocument.class));

        assertStartTag(reader.nextEvent(), "a");
        assertStartTag(reader.nextEvent(), "b");
        assertStartTag(reader.nextEvent(), "c");
        assertCharacters(reader.nextEvent(), is("test"));
        assertEndTag(reader.nextEvent(), "c");

        expectedException.expect(ParsingException.class);
        StaxParserUtil.bypassElementBlock(reader, "d");
    }

    @Test
    public void testBypassElementBlockNestedPrematureEnd() throws XMLStreamException, ParsingException {
        String xml = "<a><b><c>test</c>"
          + "<d>aa<d>nestedD</d></d>";
        XMLEventReader reader = StaxParserUtil.getXMLEventReader(IOUtils.toInputStream(xml, Charset.defaultCharset()));

        assertThat(reader.nextEvent(), instanceOf(StartDocument.class));

        assertStartTag(reader.nextEvent(), "a");
        assertStartTag(reader.nextEvent(), "b");
        assertStartTag(reader.nextEvent(), "c");
        assertCharacters(reader.nextEvent(), is("test"));
        assertEndTag(reader.nextEvent(), "c");

        StaxParserUtil.bypassElementBlock(reader, "d");

        expectedException.expect(XMLStreamException.class);
        reader.nextEvent();
    }

    @Test
    public void testGetDOMElementSameElements() throws XMLStreamException, ParsingException {
        String xml = "<root><test><test><a>b</a></test></test></root>";
        XMLEventReader reader = StaxParserUtil.getXMLEventReader(IOUtils.toInputStream(xml, Charset.defaultCharset()));

        assertThat(reader.nextEvent(), instanceOf(StartDocument.class));

        assertStartTag(reader.nextEvent(), "root");

        Element element = StaxParserUtil.getDOMElement(reader);

        assertThat(element.getNodeName(), is("test"));
        assertThat(element.getChildNodes().getLength(), is(1));

        assertThat(element.getChildNodes().item(0), instanceOf(Element.class));
        Element e = (Element) element.getChildNodes().item(0);
        assertThat(e.getNodeName(), is("test"));

        assertThat(e.getChildNodes().getLength(), is(1));
        assertThat(e.getChildNodes().item(0), instanceOf(Element.class));
        Element e1 = (Element) e.getChildNodes().item(0);
        assertThat(e1.getNodeName(), is("a"));

        assertThat(e1.getChildNodes().getLength(), is(1));
        assertThat(e1.getChildNodes().item(0), instanceOf(Text.class));
        assertThat(((Text) e1.getChildNodes().item(0)).getWholeText(), is("b"));

        assertEndTag(reader.nextEvent(), "root");
        assertThat(reader.nextEvent(), instanceOf(EndDocument.class));

        expectedException.expect(NoSuchElementException.class);
        Assert.fail(String.valueOf(reader.nextEvent()));
    }

    @Test
    public void testXMLBombAttack() throws XMLStreamException {
        String xml = "<?xml version=\"1.0\"?>" +
                "<!DOCTYPE lolz [" +
                " <!ENTITY lol \"lol\">" +
                " <!ELEMENT lolz (#PCDATA)>" +
                " <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">" +
                " <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">" +
                " <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">" +
                " <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">" +
                " <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">" +
                " <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">" +
                " <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">" +
                " <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">" +
                " <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">" +
                "]>" +
                "<lolz>&lol9;</lolz>";

        XMLEventReader reader = StaxParserUtil.getXMLEventReader(IOUtils.toInputStream(xml, Charset.defaultCharset()));

        reader.nextEvent(); // <?xml version="1.0"?>
        reader.nextEvent(); // <!DOCTYPE lolz [ ........ ]>
        try {
            StaxParserUtil.getDOMElement(reader);
        } catch (ParsingException exception) {
            // DTD should be disabled for SAML Parser, therefore this should fail with following error message
            assertThat("", exception.getMessage(), containsString("The entity \"lol9\" was referenced, but not declared"));
        }
    }

}
