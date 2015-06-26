package org.keycloak.testsuite.arquillian;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AppServerContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContext;

public class URLProvider extends URLResourceProvider {

    public static final String LOCALHOST_ADDRESS = "127.0.0.1";
    public static final String LOCALHOST_HOSTNAME = "localhost";

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        URL url = (URL) super.doLookup(resource, qualifiers);
        if (url == null) {
            try {
                for (Annotation a : qualifiers) {
                    if (AuthServerContext.class.isAssignableFrom(a.annotationType())) {
                        return new URL(getAuthServerContextRoot());
                    }
                    if (AppServerContext.class.isAssignableFrom(a.annotationType())) {
                        return new URL(getAppServerContextRoot());
                    }
                }
            } catch (MalformedURLException ex) {
                throw new IllegalStateException("Cannot inject context root.", ex);
            }
        } else {
            try {
                url = fixLocalhost(url);
                url = removeTrailingSlash(url);
            } catch (MalformedURLException ex) {
                Logger.getLogger(URLProvider.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Fixed injected @ArquillianResource URL to: " + url);
        }
        return url;
    }

    public static String getAuthServerContextRoot() {
        // TODO find if this can be extracted from ARQ metadata instead of System properties
        return "http://localhost:" + Integer.parseInt(
                System.getProperty("auth.server.http.port", "8180"));
    }

    private static String getAppServerContextRoot() {
        return "http://localhost:" + Integer.parseInt(
                System.getProperty("app.server.http.port", "8280"));
    }

    public URL fixLocalhost(URL url) throws MalformedURLException {
        URL fixedUrl = url;
        if (url.getHost().contains(LOCALHOST_ADDRESS)) {
            fixedUrl = new URL(fixedUrl.toExternalForm().replace(LOCALHOST_ADDRESS, LOCALHOST_HOSTNAME));
        }
        return fixedUrl;
    }

    public URL removeTrailingSlash(URL url) throws MalformedURLException {
        URL urlWithoutSlash = url;
        String urlS = url.toExternalForm();
        if (urlS.endsWith("/")) {
            urlWithoutSlash = new URL(urlS.substring(0, urlS.length() - 1));
        }
        return urlWithoutSlash;
    }

}
