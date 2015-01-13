package org.keycloak.account.freemarker.model;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FeaturesBean {

    private final boolean identityFederation;
    private final boolean log;
    private final boolean passwordUpdateSupported;

    public FeaturesBean(boolean identityFederation, boolean log, boolean passwordUpdateSupported) {
        this.identityFederation = identityFederation;
        this.log = log;
        this.passwordUpdateSupported = passwordUpdateSupported;
    }

    public boolean isIdentityFederation() {
        return identityFederation;
    }

    public boolean isLog() {
        return log;
    }

    public boolean isPasswordUpdateSupported() {
        return passwordUpdateSupported;
    }
}
