package org.keycloak.services.utils;

import java.util.ServiceLoader;

import org.keycloak.models.ModelProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ModelProviderUtils {

    public static final String MODEL_PROVIDER = "keycloak.model";
    public static final String DEFAULT_MODEL_PROVIDER = "jpa";

    public static Iterable<ModelProvider> getRegisteredProviders() {
        return ServiceLoader.load(ModelProvider.class);
    }

    public static ModelProvider getConfiguredModelProvider(Iterable<ModelProvider> providers) {
        String configuredProvider = System.getProperty(MODEL_PROVIDER);
        ModelProvider provider = null;

        if (configuredProvider != null) {
            for (ModelProvider p : providers) {
                if (p.getId().equals(configuredProvider)) {
                    provider = p;
                }
            }
        } else {
            for (ModelProvider p : providers) {
                if (provider == null) {
                    provider = p;
                }

                if (p.getId().equals(DEFAULT_MODEL_PROVIDER)) {
                    provider = p;
                    break;
                }
            }
        }

        return provider;
    }

    public static ModelProvider getConfiguredModelProvider() {
        return getConfiguredModelProvider(getRegisteredProviders());
    }


}
