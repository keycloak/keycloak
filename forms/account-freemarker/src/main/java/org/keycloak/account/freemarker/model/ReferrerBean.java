package org.keycloak.account.freemarker.model;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ReferrerBean {

    private String[] referrer;

    public ReferrerBean(String[] referrer) {
        this.referrer = referrer;
    }

    public String getName() {
        return referrer[0];
    }

    public String getUrl() {
        return referrer[1];
    }

}
