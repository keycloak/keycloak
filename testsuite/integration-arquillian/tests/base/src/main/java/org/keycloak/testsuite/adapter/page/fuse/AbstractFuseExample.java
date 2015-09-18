package org.keycloak.testsuite.adapter.page.fuse;

import java.net.MalformedURLException;
import java.net.URL;
import org.keycloak.testsuite.adapter.page.AppServerContextRoot;

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
