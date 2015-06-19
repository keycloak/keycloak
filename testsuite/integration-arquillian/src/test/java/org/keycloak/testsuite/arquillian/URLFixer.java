package org.keycloak.testsuite.arquillian;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;

/**
 *
 * @author tkyjovsk
 */
public class URLFixer extends URLResourceProvider {

    public static final String LOCALHOST_ADDRESS = "127.0.0.1";
    public static final String LOCALHOST_HOSTNAME = "localhost";

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        URL url = (URL) super.doLookup(resource, qualifiers);
        if (url != null) {
            try {
                if (url.getHost().contains(LOCALHOST_ADDRESS)) {
                    URL fixedUrl = new URL(url.toExternalForm().replace(LOCALHOST_ADDRESS, LOCALHOST_HOSTNAME));
                    System.out.println("Fixed URL from " + url + " to " + fixedUrl);
                    url = fixedUrl;
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(URLFixer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return url;
    }

}
