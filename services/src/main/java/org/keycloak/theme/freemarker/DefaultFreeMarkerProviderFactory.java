package org.keycloak.theme.freemarker;

import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.KeycloakSanitizerMethod;

import freemarker.template.Template;

public class DefaultFreeMarkerProviderFactory implements FreeMarkerProviderFactory {

    private volatile DefaultFreeMarkerProvider provider;
    private ConcurrentHashMap<String, Template> cache;
    private KeycloakSanitizerMethod kcSanitizeMethod;

    @Override
    public DefaultFreeMarkerProvider create(KeycloakSession session) {
        if (provider == null) {
            synchronized (this) {
                if (provider == null) {
                    if (Config.scope("theme").getBoolean("cacheTemplates", true)) {
                        cache = new ConcurrentHashMap<>();
                    }
                    kcSanitizeMethod = new KeycloakSanitizerMethod();
                    provider = new DefaultFreeMarkerProvider(cache, kcSanitizeMethod);
                }
            }
        }
        return provider;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "default";
    }

}
