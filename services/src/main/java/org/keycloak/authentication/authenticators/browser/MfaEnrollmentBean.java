package org.keycloak.authentication.authenticators.browser;

import org.keycloak.models.RequiredActionProviderModel;

import java.util.List;

public class MfaEnrollmentBean {

    private List<RequiredActionProviderModel> requiredActions;

    public MfaEnrollmentBean(List<RequiredActionProviderModel> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public List<RequiredActionProviderModel> getRequiredActions() {
        return requiredActions;
    }
}
