package org.keycloak.account.freemarker.model;

import org.keycloak.models.ApplicationModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ReferrerBean {

    private ApplicationModel referrer;

    public ReferrerBean(ApplicationModel referrer) {
        this.referrer = referrer;
    }

    public String getName() {
        return referrer.getName();
    }

    public String getBaseUrl() {
        return referrer.getBaseUrl();
    }

}
