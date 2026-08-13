package org.keycloak.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class URLEncodingTest {

    @Test
    public void testUrlEncoding() {
        Assertions.assertEquals("some", Encode.urlEncode("some"));
        Assertions.assertEquals("330cbb52-c3eb-4c4a-9f23-77a8094cd969", Encode.urlEncode("330cbb52-c3eb-4c4a-9f23-77a8094cd969"));
        Assertions.assertEquals("sp%C3%A9cial.char", Encode.urlEncode("spécial.char"));
        Assertions.assertEquals("sp%C3%A9cial.ch%2Far.%C5%BE%C3%BD%C3%A1%C3%A1", Encode.urlEncode("spécial.ch/ar.žýáá"));
    }

    @Test
    public void testUrlDecoding() {
        Assertions.assertEquals("some", Encode.urlDecode("some"));
        Assertions.assertEquals("330cbb52-c3eb-4c4a-9f23-77a8094cd969", Encode.urlDecode("330cbb52-c3eb-4c4a-9f23-77a8094cd969"));
        Assertions.assertEquals("spécial.char", Encode.urlDecode("sp%C3%A9cial.char"));
        Assertions.assertEquals("spécial.ch/ar.žýáá", Encode.urlDecode("sp%C3%A9cial.ch%2Far.%C5%BE%C3%BD%C3%A1%C3%A1"));
    }
}
