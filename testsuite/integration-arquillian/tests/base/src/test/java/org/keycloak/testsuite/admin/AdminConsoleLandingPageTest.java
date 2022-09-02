package org.keycloak.testsuite.admin;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
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
        String body = SimpleHttp.doGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/admin/master/console", client).asString();

        String authUrl = body.substring(body.indexOf("var authUrl = '") + 15);
        authUrl = authUrl.substring(0, authUrl.indexOf("'"));
        Assert.assertEquals(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", authUrl);

        String resourceUrl = body.substring(body.indexOf("var resourceUrl = '") + 19);
        resourceUrl = resourceUrl.substring(0, resourceUrl.indexOf("'"));
        Assert.assertTrue(resourceUrl.matches("/auth/resources/[^/]*/admin/([a-z]*|[a-z]*-[a-z]*)"));

        String consoleBaseUrl = body.substring(body.indexOf("var consoleBaseUrl = '") + 22);
        consoleBaseUrl = consoleBaseUrl.substring(0, consoleBaseUrl.indexOf("'"));
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

}
