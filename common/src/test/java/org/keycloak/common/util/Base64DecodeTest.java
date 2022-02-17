package org.keycloak.common.util;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Test for BASE64 decode implementation.
 *
 * @author <a href="mailto:keycloak-hc@halfbrodt.org">Hans-Christian Halfbrodt</a>
 */
public class Base64DecodeTest {

    @Test
    public void decode_simple() throws IOException {
        // high level string variant
        final String testData = "test data";
        final String encoded = "dGVzdCBkYXRh";
        final String decoded = new String(Base64.decode(encoded));
        assertThat(decoded, equalTo(testData));

        // low level byte array variant
        final byte[] encoded2 = encoded.getBytes();
        final String decoded2 = new String(Base64.decode(encoded2));
        assertThat(decoded2, equalTo(testData));
    }

    @Test
    public void decode_doNotUseGzip() throws IOException {
        // Input has gzip magic byte by coincidence and should not be gunzipped. (KEYCLOAK-18914)
        // high level string variant
        final byte[] testData = new byte[]{
                31, -117, 8, -56, 1, 1, 1, 1, 1, 1, 43, 73, 45, 46,
                81, 72, 73, 44, 73, 4, 1, -78, -82, 8, -45, 9, 1, 1, 1};
        final String encoded = "H4sIyAEBAQEBAStJLS5RSEksSQQBsq4I0wkBAQE=";
        final byte[] decoded = Base64.decode(encoded);
        assertThat(decoded, equalTo(testData));

        // low level byte array variant
        final byte[] encoded2 = encoded.getBytes();
        final byte[] decoded2 = Base64.decode(encoded2);
        assertThat(decoded2, equalTo(testData));
    }

    @Test
    public void decode_gzip() throws IOException {
        // high level string variant
        final String testData = "test data";
        final String encoded = "H4sIAAAAAAAAACtJLS5RSEksSQQAsq4I0wkAAAA=";
        final String decoded = new String(Base64.decode(encoded, Base64.GUNZIP));
        assertThat(decoded, equalTo(testData));

        // low level byte array variant
        // specified to ignore gunzip option (see javadoc)
        final byte[] expected2 = new byte[]{
                31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 43, 73,
                45, 46, 81, 72, 73, 44, 73, 4, 0, -78, -82,
                8, -45, 9, 0, 0, 0};
        final byte[] encoded2 = encoded.getBytes();
        final byte[] decoded2 = Base64.decode(encoded2, 0, encoded2.length, Base64.GUNZIP);
        assertThat(decoded2, equalTo(expected2));
    }

    @Test
    public void decode_empty() throws IOException {
        final byte[] result = Base64.decode("");
        assertThat(result, equalTo(new byte[0]));
        final byte[] result2 = Base64.decode(new byte[0]);
        assertThat(result2, equalTo(new byte[0]));

        try {
            Base64.decode(" ");
            MatcherAssert.assertThat("Exception excepted", false);
        } catch (final Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }

        try {
            Base64.decode(" ".getBytes());
            MatcherAssert.assertThat("Exception excepted", false);
        } catch (final Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }

        try {
            Base64.decode((String) null);
            MatcherAssert.assertThat("Exception excepted", false);
        } catch (final Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }

        try {
            Base64.decode((byte[]) null);
            MatcherAssert.assertThat("Exception excepted", false);
        } catch (final Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }
    }

    @Test
    public void decode_lowLevelInvalidParams() {
        try {
            Base64.decode(null, 0, 1, Base64.NO_OPTIONS);
            MatcherAssert.assertThat("Exception excepted", false);
        } catch (final Exception e){
            assertThat(e, instanceOf(NullPointerException.class));
        }

        try {
            Base64.decode(new byte[2], 0, 1, Base64.NO_OPTIONS);
            MatcherAssert.assertThat("Exception excepted", false);
        } catch (final Exception e){
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }

        try {
            Base64.decode(new byte[8], 0, 10, Base64.NO_OPTIONS);
            MatcherAssert.assertThat("Exception excepted", false);
        } catch (final Exception e){
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }

        try {
            Base64.decode(new byte[8], 5, 5, Base64.NO_OPTIONS);
            MatcherAssert.assertThat("Exception excepted", false);
        } catch (final Exception e){
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }
}
