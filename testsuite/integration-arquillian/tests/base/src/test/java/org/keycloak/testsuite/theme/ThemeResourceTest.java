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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class ThemeResourceTest extends AbstractTestRealmKeycloakTest {
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void shouldFetchEntireMessageBundle() {
        Properties messageBundle = getMessageBundle("en", null, null, null);

        assertFalse("Message list should not be empty", messageBundle.isEmpty());
        assertTrue("Should be the entire bundle", messageBundle.size() > 300);
        assertEquals("Save", messageBundle.getProperty("save"));
    }

    @Test
    public void shouldFetchPaginated() {
        Properties messageBundle = getMessageBundle("en", 0L, 10L, null);

        assertFalse("Message list should not be empty", messageBundle.isEmpty());
        assertEquals("Should be paginated", 10, messageBundle.size());
    }

    @Test
    public void shouldFetchGerman() {
        Properties messageBundle = getMessageBundle("de", null, null, null);

        assertFalse("Message list should not be empty", messageBundle.isEmpty());
        assertEquals("Speichern", messageBundle.getProperty("save"));
    }

    @Test
    public void shouldFetchFilterOnMessagesHavingWords() {
        Properties messageBundle = getMessageBundle("en", 0L, null, List.of("nothing", "cancel"));

        for (Object k : messageBundle.keySet()) {
            final String property = messageBundle.getProperty(k.toString());
            boolean hasWord = property.toLowerCase().contains("nothing") || property.toLowerCase().contains("cancel");
            assertTrue(String.format("Key %s with value %s should contain nothing or cancel", k, property), hasWord);
        }
    }

    private Properties getMessageBundle(String locale, Long first, Long max, List<String> hasWords) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableContentCompression().build()) {
            final StringBuilder url = new StringBuilder();
            url.append(suiteContext.getAuthServerInfo().getContextRoot().toString()).append("/auth/resources/")
                    .append("master").append("/").append("admin").append("/").append(locale);
            if (first != null) {
                url.append("?first=").append(first);
            }
            if (max != null) {
                url.append("&max=").append(max);
            }
            if (hasWords != null) {
                for (String word : hasWords) {
                    url.append("&hasWords=").append(word);
                }
            }
            HttpGet get = new HttpGet(url.toString());
            CloseableHttpResponse response = httpClient.execute(get);

            InputStream is = response.getEntity().getContent();
            assertEquals(200, response.getStatusLine().getStatusCode());

            final JsonNode node = JsonSerialization.mapper.readTree(is);
            final Iterator<JsonNode> iterator = node.iterator();
            Properties props = new Properties();
            while (iterator.hasNext()) {
                final JsonNode jsonNode = iterator.next();
                props.put(jsonNode.get("key").asText(), jsonNode.get("value").asText());
            }
            return props;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
