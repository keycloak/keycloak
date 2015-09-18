package org.keycloak.testsuite.page;

import java.net.URISyntaxException;
import java.net.URL;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractPageWithInjectedUrl extends AbstractPage {

    public abstract URL getInjectedUrl();

    @Override
    public UriBuilder createUriBuilder() {
        try {
            return UriBuilder.fromUri(getInjectedUrl().toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
