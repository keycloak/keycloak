package org.keycloak.testsuite.theme;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThemeResourceTest extends AbstractTestRealmKeycloakTest {
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void shouldFetchEntireMessageBundle() {
        Map<String, String> messageBundle = getMessageBundle("en", null, null);

        assertThat("Message list should not be empty", messageBundle, not(anEmptyMap()));
        assertThat("Should be the entire bundle", messageBundle, aMapWithSize(greaterThan(300)));
        assertEquals("Save", messageBundle.get("save"));
    }

    @Test
    public void shouldFetchPaginated() {
        Map<String, String> messageBundle = getMessageBundle("en", 0, 10);

        assertThat("Message list should not be empty", messageBundle, not(anEmptyMap()));
        assertThat("Should be paginated", messageBundle, aMapWithSize(10));

        // assert that the first 10 keys are the same as the first 10 keys of the entire bundle
        Map<String, String> messageBundle2 = getMessageBundle("en", 0, 100);
        assertThat("Should be paginated", messageBundle2, aMapWithSize(100));
        for (Object varName : messageBundle.keySet()) {
            final String key = varName.toString();
            assertEquals("Key " + key + " should be the same in both bundles", messageBundle.get(key), messageBundle2.get(key));
        }

        // assert that the first 10 keys are not the same as the second 10 keys of the entire bundle
        Map<String, String> messageBundle3 = getMessageBundle("en", 10, 100);
        assertThat("Should be paginated", messageBundle3, aMapWithSize(100));
        for (Map.Entry<String, String> me : messageBundle.entrySet()) {
            final String key = me.getKey();
            final String value = me.getValue();
            assertThat("Key " + key + " should not be the same in both bundles", messageBundle3, not(hasEntry(key, value)));
        }
    }

    @Test
    public void shouldFetchGerman() {
        Map<String, String> messageBundle = getMessageBundle("de", null, null);

        assertThat("Message list should not be empty", messageBundle, not(anEmptyMap()));
        assertEquals("Speichern", messageBundle.get("save"));
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWrongRealm() {
        getMessageBundle("nonexistent-realm", "de", null, null);
    }

    @Test
    public void shouldFetchFilterOnMessagesHavingWords() {
        Map<String, String> messageBundle = getMessageBundle("en", 0, null, "nothing", "cancel");

        for (Object k : messageBundle.keySet()) {
            final String property = messageBundle.get(k.toString());
            boolean hasWord = property.toLowerCase().contains("nothing") || property.toLowerCase().contains("cancel");
            assertTrue(String.format("Key %s with value %s should contain nothing or cancel", k, property), hasWord);
        }
    }

    private Map<String, String> getMessageBundle(String locale, Integer first, Integer max, String... hasWords) {
        return getMessageBundle("master", locale, first, max, hasWords);
    }

    private Map<String, String> getMessageBundle(String realm, String locale, Integer first, Integer max, String... hasWords) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableContentCompression().build()) {
            final StringBuilder url = new StringBuilder();
            url.append(suiteContext.getAuthServerInfo().getContextRoot().toString()).append("/auth/resources/")
                    .append(realm).append("/").append("admin").append("/").append(locale);
            if (first != null) {
                url.append("?first=").append(first);
            }
            if (max != null) {
                url.append("&max=").append(max);
            }
            for (String word : hasWords) {
                url.append("&hasWords=").append(word);
            }
            HttpGet get = new HttpGet(url.toString());
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                InputStream is = response.getEntity().getContent();
                assertEquals(200, response.getStatusLine().getStatusCode());

                final JsonNode node = JsonSerialization.mapper.readTree(is);
                final Iterator<JsonNode> iterator = node.iterator();
                Map<String, String> props = new HashMap<>();
                while (iterator.hasNext()) {
                    final JsonNode jsonNode = iterator.next();
                    props.put(jsonNode.get("key").asText(), jsonNode.get("value").asText());
                }
                return props;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
