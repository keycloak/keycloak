package org.keycloak.testsuite.page;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractPageWithInjectedUrl extends AbstractPage {

    public abstract URL getInjectedUrl();

    //EAP6 URL fix
    protected URL createInjectedURL(String url) {
        if (System.getProperty("app.server.eap6","false").equals("false")) {
            return null;
        }
        try {
            if(Boolean.parseBoolean(System.getProperty("app.server.ssl.required"))) {
                return new URL("https://localhost:" + System.getProperty("app.server.https.port", "8543") + "/" + url);
            };
            return new URL("http://localhost:" + System.getProperty("app.server.http.port", "8180") + "/" + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public UriBuilder createUriBuilder() {
        try {
            return UriBuilder.fromUri(getInjectedUrl().toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
