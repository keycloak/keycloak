package org.keycloak.admin.api;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.Set;

public interface CdiProviderFactory<T extends Provider> extends ProviderFactory<T> {

    Class<? extends T> getProviderClass();

    @Override
    default T create(KeycloakSession session) {
        BeanManager beanManager = CDI.current().getBeanManager();

        Set<Bean<?>> beans = beanManager.getBeans(getProviderClass(), new AnnotationLiteral<ChosenBySpi>() {});
        // might check ambiguity?
        Bean<?> matchingBean = beans.stream()
                .filter(b -> b.getBeanClass().equals(getProviderClass()))
                .findFirst()
                .orElseThrow();

        CreationalContext<?> ctx = beanManager.createCreationalContext(matchingBean);
        //noinspection unchecked
        return (T) beanManager.getReference(matchingBean, getProviderClass(), ctx);
    }
}
