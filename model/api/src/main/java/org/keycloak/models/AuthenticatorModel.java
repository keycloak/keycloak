package org.keycloak.models;

import java.util.HashMap;
import java.util.Map;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AuthenticatorModel {

    public enum Requirement {
        REQUIRED,
        OPTIONAL,
        ALTERNATIVE
    }

    private String id;
    private String alias;
    private String providerId;
    private boolean masterAuthenticator;
    private boolean formBased;
    private String inputPage;
    private String actionUrl;
    private String setupUrl;
    private Requirement requirement;
    private boolean userSetupAllowed;
    private int priority;
    private Map<String, String> config = new HashMap<String, String>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isFormBased() {
        return formBased;
    }

    public void setFormBased(boolean formBased) {
        this.formBased = formBased;
    }

    public String getInputPage() {
        return inputPage;
    }

    public void setInputPage(String inputPage) {
        this.inputPage = inputPage;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getSetupUrl() {
        return setupUrl;
    }

    public void setSetupUrl(String setupUrl) {
        this.setupUrl = setupUrl;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public void setRequirement(Requirement requirement) {
        this.requirement = requirement;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isUserSetupAllowed() {
        return userSetupAllowed;
    }

    public void setUserSetupAllowed(boolean userSetupAllowed) {
        this.userSetupAllowed = userSetupAllowed;
    }

    public boolean isMasterAuthenticator() {
        return masterAuthenticator;
    }

    public void setMasterAuthenticator(boolean masterAuthenticator) {
        this.masterAuthenticator = masterAuthenticator;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
