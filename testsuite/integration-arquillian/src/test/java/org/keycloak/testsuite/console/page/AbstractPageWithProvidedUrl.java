package org.keycloak.testsuite.console.page;

import java.net.URISyntaxException;
import java.net.URL;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractPageWithProvidedUrl extends AbstractPage {

    public abstract URL getProvidedUrl();

    @Override
    public UriBuilder createUriBuilder() {
        try {
            return UriBuilder.fromUri(getProvidedUrl().toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
