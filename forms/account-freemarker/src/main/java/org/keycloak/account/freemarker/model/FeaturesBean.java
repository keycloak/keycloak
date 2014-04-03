package org.keycloak.account.freemarker.model;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FeaturesBean {

    private final boolean social;
    private final boolean log;

    public FeaturesBean(boolean social, boolean log) {
        this.social = social;
        this.log = log;
    }

    public boolean isSocial() {
        return social;
    }

    public boolean isLog() {
        return log;
    }

}
