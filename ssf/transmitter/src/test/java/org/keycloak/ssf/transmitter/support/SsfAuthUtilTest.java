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
    void quoteStripsOtherControlCharacters() {
        assertEquals("\"realm\"", SsfAuthUtil.quote("re\u0000a\u001Bl\u007Fm"));
    }

    @Test
    void quoteKeepsNonAscii() {
        assertEquals("\"r\u00e9alm\"", SsfAuthUtil.quote("r\u00e9alm"));
    }

    @Test
    void quoteHandlesNull() {
        assertEquals("\"\"", SsfAuthUtil.quote(null));
    }
}
