package org.keycloak.testsuite.page.adapter.fuse;

import java.net.MalformedURLException;
import java.net.URL;
import org.keycloak.testsuite.page.adapter.AppServerContextRoot;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractFuseExample extends AppServerContextRoot {

    public abstract String getContext();

    private URL url;

    @Override
    public URL getInjectedUrl() {
        if (url == null) {
            try {
                url = new URL(super.getInjectedUrl().toExternalForm() + "/" + getContext());
            } catch (MalformedURLException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return url;
    }

}
