package org.keycloak.adapters.saml.descriptor.parsers;

import java.io.InputStream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.saml.common.exceptions.ParsingException;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SamlDescriptorIDPKeysExtractorTest {

  @Test
  public void testParsingFileContainingEntityDescriptorAsRootElement() {
    testParse("saml-idp-metadata-with-entity-descriptor-as-root-element.xml");
  }

  @Test
  public void testParsingFileContainingEntitiesDescriptorAsRootElement() {
    testParse("saml-idp-metadata-with-entities-descriptor-as-root-element.xml");
  }


  public void testParse(String fileToParse) {
    InputStream stream = getClass().getResourceAsStream(fileToParse);
    SamlDescriptorIDPKeysExtractor extractor = new SamlDescriptorIDPKeysExtractor();
    try {
      MultivaluedHashMap keyMap = extractor.parse(stream);
      assertFalse(keyMap.isEmpty());
      assertTrue(keyMap.containsKey("signing"));
      assertTrue(keyMap.containsKey("encryption"));
    } catch (ParsingException e) {
      fail(e.getMessage());
    }
  }


}
