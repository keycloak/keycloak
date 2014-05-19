package org.keycloak.email.freemarker;

import org.keycloak.Config;
import org.keycloak.email.EmailProvider;
import org.keycloak.email.EmailProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerEmailProviderFactory implements EmailProviderFactory {

    @Override
    public EmailProvider create(ProviderSession providerSession) {
        return new FreeMarkerEmailProvider(providerSession);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "freemarker";
    }

}
