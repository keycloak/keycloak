package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminConsoleLandingPageTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void landizngPage() throws IOException {
        String body = SimpleHttp.doGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/admin/master/console").asString();

        String authUrl = body.substring(body.indexOf("var authUrl = '") + 15);
        authUrl = authUrl.substring(0, authUrl.indexOf("'"));
        Assert.assertEquals("/auth", authUrl);

        String resourceUrl = body.substring(body.indexOf("var resourceUrl = '") + 19);
        resourceUrl = resourceUrl.substring(0, resourceUrl.indexOf("'"));
        Assert.assertTrue(resourceUrl.matches("/auth/resources/[^/]*/admin/[a-z]*"));

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
