/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.ByteArrayInputStream;
import java.lang.RuntimeException;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAML11AssertionParser;
import org.keycloak.saml.processing.core.parsers.saml.SAML11RequestParser;
import org.keycloak.saml.processing.core.parsers.saml.SAML11ResponseParser;
import org.keycloak.saml.processing.core.parsers.saml.SAML11SubjectParser;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;

/**
  This fuzzer targets the parse method of all five SAML parser,
  including SAMLParser, SAML11SubjectParser, SAML11ResponseParser,
  SAML11RequestParser, SAML11AssertionParser. It creates a
  XMLEventReader with random bytes in UTF-8 encoding and
  pass it as a source for the a random SAML parser to parse it.
  */
public class SamlParserFuzzer {
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    try {
      // Randomize which SAML Parser is used
      Integer choice = data.consumeInt(1, 5);

      // Initialize a XMLEventReader with InputStream source pointing
      // to a random byte array in UTF-8 encoding retrieved from the
      // FuzzedDataProvider
      byte[] input = data.consumeRemainingAsString().getBytes(StandardCharsets.UTF_8);
      ByteArrayInputStream bais = new ByteArrayInputStream(input);
      XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(bais);

      // Retrieve or create a random SAML Parser object
      // instance and run the parse method with the
      // random data provided by the XMLEventReader
      // object created above
      switch (choice) {
        case 1:
          SAMLParser.getInstance().parse(reader);
          break;
        case 2:
          new SAML11SubjectParser().parse(reader);
          break;
        case 3:
          new SAML11ResponseParser().parse(reader);
          break;
        case 4:
          new SAML11RequestParser().parse(reader);
          break;
        case 5:
          new SAML11AssertionParser().parse(reader);
          break;
      }
    } catch (ParsingException | XMLStreamException | RuntimeException e) {
      // Known exception
    }
  }
}
