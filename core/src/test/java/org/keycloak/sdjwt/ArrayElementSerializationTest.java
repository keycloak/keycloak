package org.keycloak.sdjwt;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class ArrayElementSerializationTest {

    @Before
    public void setUp() throws Exception {
        SdJwtUtils.arrayEltSpaced = false;
    }

    @After
    public void tearDown() throws Exception {
        SdJwtUtils.arrayEltSpaced = true;
    }

    @Test
    public void testToBase64urlEncoded() {
        // Create an instance of UndisclosedArrayElement with the specified fields
        // "lklxF5jMYlGTPUovMNIvCA", "FR"
        UndisclosedArrayElement arrayElementDisclosure = UndisclosedArrayElement.builder()
                .withSalt(new SdJwtSalt("lklxF5jMYlGTPUovMNIvCA"))
                .withArrayElement(new TextNode("FR")).build();

        // Expected Base64 URL encoded string
        String expected = "WyJsa2x4RjVqTVlsR1RQVW92TU5JdkNBIiwiRlIiXQ";

        // Assert that the base64 URL encoded string from the object matches the
        // expected string
        assertEquals(expected, arrayElementDisclosure.getDisclosureString());
    }
}
