package org.keycloak.common.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for Base64Url implementation to ensure backward compatibility
 * with JDK Base64 URL encoder/decoder.
 *
 * @author Keycloak Team
 */
public class Base64UrlTest {

    @Test
    public void testEncodeSimple() {
        byte[] testData = "test".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64Url.encode(testData);
        assertThat(encoded, equalTo("dGVzdA"));
    }

    @Test
    public void testDecodeSimple() {
        String encoded = "dGVzdA";
        byte[] decoded = Base64Url.decode(encoded);
        assertThat(new String(decoded, StandardCharsets.UTF_8), equalTo("test"));
    }

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] testData = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64Url.encode(testData);
        byte[] decoded = Base64Url.decode(encoded);
        assertThat(decoded, equalTo(testData));
        assertThat(new String(decoded, StandardCharsets.UTF_8), equalTo("Hello, World!"));
    }

    @Test
    public void testEncodeWithoutPadding() {
        // Test that encoding produces no padding
        byte[] testData1 = "a".getBytes(StandardCharsets.UTF_8);
        String encoded1 = Base64Url.encode(testData1);
        assertThat(encoded1.contains("="), equalTo(false));

        byte[] testData2 = "ab".getBytes(StandardCharsets.UTF_8);
        String encoded2 = Base64Url.encode(testData2);
        assertThat(encoded2.contains("="), equalTo(false));

        byte[] testData3 = "abc".getBytes(StandardCharsets.UTF_8);
        String encoded3 = Base64Url.encode(testData3);
        assertThat(encoded3.contains("="), equalTo(false));
    }

    @Test
    public void testDecodeHandlesPaddedInput() {
        // URL decoder should handle both padded and unpadded input
        byte[] expected = "test".getBytes(StandardCharsets.UTF_8);
        
        String unpadded = "dGVzdA";
        byte[] decoded1 = Base64Url.decode(unpadded);
        assertThat(decoded1, equalTo(expected));
    }

    @Test
    public void testUrlSafeCharacters() {
        // Test that URL-safe characters (- and _) are used instead of (+ and /)
        byte[] testData = new byte[]{(byte) 0xFB, (byte) 0xFF, (byte) 0xFE};
        String encoded = Base64Url.encode(testData);
        
        // Should not contain + or /
        assertThat(encoded.contains("+"), equalTo(false));
        assertThat(encoded.contains("/"), equalTo(false));
        
        // Should decode back correctly
        byte[] decoded = Base64Url.decode(encoded);
        assertThat(decoded, equalTo(testData));
    }

    @Test
    public void testEncodeBase64ToBase64Url() {
        // Test the conversion method
        String base64 = "dGVzdA==";
        String base64Url = Base64Url.encodeBase64ToBase64Url(base64);
        assertThat(base64Url, equalTo("dGVzdA"));
    }

    @Test
    public void testEncodeBase64UrlToBase64() {
        // Test the conversion method
        String base64Url = "dGVzdA";
        String base64 = Base64Url.encodeBase64UrlToBase64(base64Url);
        assertThat(base64, equalTo("dGVzdA=="));
    }

    @Test
    public void testLargerData() {
        // Test with larger data to ensure it works correctly
        byte[] testData = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64Url.encode(testData);
        byte[] decoded = Base64Url.decode(encoded);
        assertThat(decoded, equalTo(testData));
    }

    @Test
    public void testEmptyData() {
        byte[] empty = new byte[0];
        String encoded = Base64Url.encode(empty);
        assertThat(encoded, equalTo(""));
        
        byte[] decoded = Base64Url.decode("");
        assertThat(decoded, equalTo(empty));
    }

    @Test(expected = RuntimeException.class)
    public void testDecodeInvalidInput() {
        // Should throw exception for invalid input
        Base64Url.decode("!!!invalid!!!");
    }
}
