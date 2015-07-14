package org.keycloak.testsuite.arquillian;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;

public class URLProvider extends URLResourceProvider {

    public static final String LOCALHOST_ADDRESS = "127.0.0.1";
    public static final String LOCALHOST_HOSTNAME = "localhost";

    @Inject
    Instance<ContextRootStore> contextRootStore;

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        URL url = (URL) super.doLookup(resource, qualifiers);

        // inject context roots if annotation present
        for (Annotation a : qualifiers) {
            if (AuthServerContainer.class.isAssignableFrom(a.annotationType())) {
                return contextRootStore.get().getAuthServerContextRoot();
            }
            if (AppServerContainer.class.isAssignableFrom(a.annotationType())) {
                return contextRootStore.get().getAppServerContextRoot();
            }
        }

        // fix injected URLs
        if (url != null) {
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
