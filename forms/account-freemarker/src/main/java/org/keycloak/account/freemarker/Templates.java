package org.keycloak.account.freemarker;

import org.keycloak.account.AccountPages;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Templates {

    public static String getTemplate(AccountPages page) {
        switch (page) {
            case ACCOUNT:
                return "account.ftl";
            case PASSWORD:
                return "password.ftl";
            case TOTP:
                return "totp.ftl";
            case FEDERATED_IDENTITY:
                return "federatedIdentity.ftl";
            case LOG:
                return "log.ftl";
            case SESSIONS:
                return "sessions.ftl";
            default:
                throw new IllegalArgumentException();
        }
    }

}
