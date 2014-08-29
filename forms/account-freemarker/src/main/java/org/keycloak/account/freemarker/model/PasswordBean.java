package org.keycloak.account.freemarker.model;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordBean {

    private boolean passwordSet;

    public PasswordBean(boolean passwordSet) {
        this.passwordSet = passwordSet;
    }

    public boolean isPasswordSet() {
        return passwordSet;
    }

}
