package org.keycloak.testsuite.adapter.page;

import org.keycloak.testsuite.page.AbstractPage;

import javax.ws.rs.core.UriBuilder;
import java.net.URL;

/**
 * @author mhajas
 */
public class HawtioPage extends AbstractPage {

    public String getUrl() {
        if (Boolean.parseBoolean(System.getProperty("app.server.ssl.required"))) {
            return "https://localhost:" + System.getProperty("app.server.https.port", "8543") + "/hawtio";
        }
        return "http://localhost:" + System.getProperty("app.server.http.port", "8180") + "/hawtio";
    }

    @Override
    public UriBuilder createUriBuilder() {
        return UriBuilder.fromUri(getUrl());
    }
}
