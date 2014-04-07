package org.keycloak.account.freemarker.model;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FeaturesBean {

    private final boolean social;
    private final boolean log;
    private final boolean passwordUpdateSupported;

    public FeaturesBean(boolean social, boolean log, boolean passwordUpdateSupported) {
        this.social = social;
        this.log = log;
        this.passwordUpdateSupported = passwordUpdateSupported;
    }

    public boolean isSocial() {
        return social;
    }

    public boolean isLog() {
        return log;
    }

    public boolean isPasswordUpdateSupported() {
        return passwordUpdateSupported;
    }
}
