package org.keycloak.email.freemarker;

import org.keycloak.Config;
import org.keycloak.email.EmailProvider;
import org.keycloak.email.EmailProviderFactory;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerEmailProviderFactory implements EmailProviderFactory {

    private FreeMarkerUtil freeMarker;

    @Override
    public EmailProvider create(ProviderSession providerSession) {
        return new FreeMarkerEmailProvider(providerSession, freeMarker);
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
    }

    @Override
    public void close() {
        freeMarker = null;
    }

    @Override
    public String getId() {
        return "freemarker";
    }

}
