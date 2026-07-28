package org.keycloak.common.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link Base64Url}.
 */
public class Base64UrlTest {

    @Test
    public void encodeBase64ToBase64Url_normalInput() {
        // Standard Base64 with padding → Base64Url without padding, charset swapped.
        assertThat(Base64Url.encodeBase64ToBase64Url("dGVzdA=="), equalTo("dGVzdA"));
        assertThat(Base64Url.encodeBase64ToBase64Url("aGVsbG8="), equalTo("aGVsbG8"));
    }

    @Test
    public void encodeBase64ToBase64Url_charsetConversion() {
        // '+' → '-', '/' → '_'
        assertThat(Base64Url.encodeBase64ToBase64Url("AB+C/D=="), equalTo("AB-C_D"));
    }

    @Test
    public void encodeBase64ToBase64Url_paddingOnlyDoesNotThrow() {
        // A padding-only input (e.g. "=" or "==") previously caused an
        // ArrayIndexOutOfBoundsException because String.split("=") discards
        // trailing empty strings and produces a zero-length array.
        assertThat(Base64Url.encodeBase64ToBase64Url("="), equalTo(""));
        assertThat(Base64Url.encodeBase64ToBase64Url("=="), equalTo(""));
        assertThat(Base64Url.encodeBase64ToBase64Url("==="), equalTo(""));
    }

    @Test
    public void encodeBase64ToBase64Url_emptyString() {
        assertThat(Base64Url.encodeBase64ToBase64Url(""), equalTo(""));
    }

    @Test
    public void decode_normalInput() {
        byte[] result = Base64Url.decode("dGVzdA");
        assertThat(new String(result, StandardCharsets.UTF_8), equalTo("test"));
    }

    @Test
    public void decode_base64WithPadding() {
        // decode() routes through encodeBase64ToBase64Url, so padded Base64
        // input must still work.
        byte[] result = Base64Url.decode("dGVzdA==");
        assertThat(new String(result, StandardCharsets.UTF_8), equalTo("test"));
    }
}
