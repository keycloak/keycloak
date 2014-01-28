package org.keycloak.login;

import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FormsLoader {

    private FormsLoader() {
    }

    public static FormsProvider load() {
        return ServiceLoader.load(FormsProvider.class).iterator().next();
    }

}
