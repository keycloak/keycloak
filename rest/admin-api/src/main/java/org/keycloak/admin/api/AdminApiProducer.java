package org.keycloak.admin.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.realm.RealmApi;
import org.keycloak.admin.api.realm.RealmsApi;
import org.keycloak.admin.api.root.AdminApi;
import org.keycloak.provider.Provider;
import org.keycloak.utils.KeycloakSessionUtil;

import java.util.Set;

/**
 * Produces CDI-managed references to the API providers selected by the SPI configuration.
 * <p>
 * The producer itself is {@code @ApplicationScoped}, but the returned beans are {@code @RequestScoped}.
 * <p>
 * This avoids resolving the provider via {@code KeycloakSession} on every injection point.
 * The selection logic is executed only once per application lifecycle, and the returned
 * reference will delegate to the actual provider implementation.
 */
@ApplicationScoped
public class AdminApiProducer {

    @Produces
    @ApplicationScoped
    AdminApi getAdminApi() {
        return getBean(AdminApi.class);
    }

    @Produces
    @ApplicationScoped
    RealmsApi getRealmsApi() {
        return getBean(RealmsApi.class);
    }

    @Produces
    @ApplicationScoped
    RealmApi getRealmApi() {
        return getBean(RealmApi.class);
    }

    @Produces
    @ApplicationScoped
    ClientsApi getClientsApi() {
        return getBean(ClientsApi.class);
    }

    @Produces
    @ApplicationScoped
    ClientApi getClientApi() {
        return getBean(ClientApi.class);
    }

    private <T extends Provider> T getBean(Class<T> apiClass) {
        BeanManager beanManager = CDI.current().getBeanManager();

        Class<?> implClass = KeycloakSessionUtil.getKeycloakSession()
                .getProvider(apiClass)
                .getClass();

        Set<Bean<?>> beans = beanManager.getBeans(apiClass, new AnnotationLiteral<ChosenBySpi>() {
        });
        Bean<?> matchingBean = beans.stream()
                .filter(b -> b.getBeanClass().equals(implClass))
                .findFirst()
                .orElseThrow();

        CreationalContext<?> ctx = beanManager.createCreationalContext(matchingBean);
        //noinspection unchecked
        return (T) beanManager.getReference(matchingBean, apiClass, ctx);
    }
}
