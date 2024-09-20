package org.keycloak.testsuite.admin;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminConsoleLandingPageTest extends AbstractKeycloakTest {

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void landingPage() throws IOException {
        String body = SimpleHttpDefault.doGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/admin/master/console", client).asString();

        Map<String, String> config = getConfig(body);
        String authUrl = config.get("authUrl");
        Assert.assertEquals(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", authUrl);

        String resourceUrl = config.get("resourceUrl");
        Assert.assertTrue(resourceUrl.matches("/auth/resources/[^/]*/admin/keycloak.v2"));

        String consoleBaseUrl = config.get("consoleBaseUrl");
        Assert.assertEquals(consoleBaseUrl, "/auth/admin/master/console/");

        Pattern p = Pattern.compile("link href=\"([^\"]*)\"");
        Matcher m = p.matcher(body);

        while(m.find()) {
                String url = m.group(1);
                Assert.assertTrue(url.startsWith("/auth/resources/"));
        }

        p = Pattern.compile("script src=\"([^\"]*)\"");
        m = p.matcher(body);

        while(m.find()) {
            String url = m.group(1);
            if (url.contains("keycloak.js")) {
                Assert.assertTrue(url, url.startsWith("/auth/js/"));
            } else {
                Assert.assertTrue(url, url.startsWith("/auth/resources/"));
            }
        }
    }

    private static Map<String, String> getConfig(String body) {
        Map<String, String> variables = new HashMap<>();
        String start = "<script id=\"environment\" type=\"application/json\">";
        String end = "</script>";

        String config = body.substring(body.indexOf(start) + start.length());
        config = config.substring(0, config.indexOf(end)).trim();

        Matcher matcher = Pattern.compile(".*\"(.*)\": \"(.*)\"").matcher(config);
        while (matcher.find()) {
            variables.put(matcher.group(1), matcher.group(2));
        }

        return variables;
    }

}
