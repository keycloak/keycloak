package org.keycloak.testsuite.arquillian.provider;

import org.keycloak.testsuite.arquillian.TestContext;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.keycloak.testsuite.arquillian.annotation.AppServerContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContext;

public class URLProvider extends URLResourceProvider {

    protected final Logger log = Logger.getLogger(this.getClass());

    public static final String LOCALHOST_ADDRESS = "127.0.0.1";
    public static final String LOCALHOST_HOSTNAME = "localhost";

    @Inject
    Instance<TestContext> testContext;

    private static final Set<String> fixedUrls = new HashSet<>();

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        URL url = (URL) super.doLookup(resource, qualifiers);

        // fix injected URL
        if (url != null) {
            try {
                url = fixLocalhost(url);
                url = removeTrailingSlash(url);
            } catch (MalformedURLException ex) {
                log.log(Level.FATAL, null, ex);
            }

            if (!fixedUrls.contains(url.toString())) {
                fixedUrls.add(url.toString());
                log.debug("Fixed injected @ArquillianResource URL to: " + url);
            }
        }

        // inject context roots if annotation present
        for (Annotation a : qualifiers) {
            if (AuthServerContext.class.isAssignableFrom(a.annotationType())) {
                return testContext.get().getAuthServerContextRoot();
            }
            if (AppServerContext.class.isAssignableFrom(a.annotationType())) {
                return testContext.get().getAppServerContextRoot();
            }
        }

        return url;
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
