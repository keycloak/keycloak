package org.keycloak.login;

import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginFormsLoader {

    private LoginFormsLoader() {
    }

    public static LoginFormsProvider load() {
        return ServiceLoader.load(LoginFormsProvider.class).iterator().next();
    }

}
