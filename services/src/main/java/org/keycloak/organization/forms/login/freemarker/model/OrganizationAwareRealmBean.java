package org.keycloak.organization.forms.login.freemarker.model;

import org.keycloak.forms.login.freemarker.model.RealmBean;
import org.keycloak.models.RealmModel;

public class OrganizationAwareRealmBean extends RealmBean {

    public OrganizationAwareRealmBean(RealmModel realmModel) {
        super(realmModel);
    }

    @Override
    public boolean isRegistrationAllowed() {
        return false;
    }
}
