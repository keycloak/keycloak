package org.keycloak.ssf.transmitter.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SsfAuthUtilTest {

    @Test
    void quoteWrapsPlainValue() {
        assertEquals("\"ssf-poc\"", SsfAuthUtil.quote("ssf-poc"));
    }

    @Test
    void quoteEscapesQuotesAndBackslashes() {
        assertEquals("\"a\\\"b\\\\c\"", SsfAuthUtil.quote("a\"b\\c"));
    }

    @Test
    void quoteStripsLineBreaks() {
        assertEquals("\"realm injected\"", SsfAuthUtil.quote("realm\r\n injected"));
    }

    @Test
    void quoteHandlesNull() {
        assertEquals("\"\"", SsfAuthUtil.quote(null));
    }
}
