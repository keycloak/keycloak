package org.keycloak.account;

import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountLoader {

    private AccountLoader() {
    }

    public static AccountProvider load() {
        return ServiceLoader.load(AccountProvider.class).iterator().next();
    }

}
