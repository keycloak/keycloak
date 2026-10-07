package org.keycloak.tests.providers.email;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * Wraps the built-in email templates and records which reset password methods are called, so tests can check that
 * every reset password request does the same work, without measuring time. Off by default, turn it on with
 * {@code spi-email-template--counting-freemarker--enabled=true}.
 */
public class CountingEmailTemplateProviderFactory implements EmailTemplateProviderFactory, EnvironmentDependentProviderFactory {

    public static final String ID = "counting-freemarker";

    private static final Set<String> RECORDED = Set.of("renderPasswordReset", "queue", "sendPasswordReset");

    private final List<String> operations = new CopyOnWriteArrayList<>();

    @Override
    public EmailTemplateProvider create(KeycloakSession session) {
        EmailTemplateProvider delegate = session.getProvider(EmailTemplateProvider.class, "freemarker");
        return (EmailTemplateProvider) Proxy.newProxyInstance(EmailTemplateProvider.class.getClassLoader(),
                new Class<?>[] { EmailTemplateProvider.class }, (proxy, method, args) -> {
                    if (RECORDED.contains(method.getName())) {
                        operations.add(method.getName());
                    }
                    Object result;
                    try {
                        result = method.invoke(delegate, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                    // return the wrapper from the setters, so the calls that follow are recorded too
                    return result == delegate ? proxy : result;
                });
    }

    public static List<String> getAndResetOperations(KeycloakSession session) {
        CountingEmailTemplateProviderFactory factory = (CountingEmailTemplateProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(EmailTemplateProvider.class, ID);
        List<String> recorded = List.copyOf(factory.operations);
        factory.operations.clear();
        return recorded;
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
        return ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return config.getBoolean("enabled", false);
    }

    // higher than the built-in provider, so this one is used when turned on
    @Override
    public int order() {
        return 1;
    }
}
