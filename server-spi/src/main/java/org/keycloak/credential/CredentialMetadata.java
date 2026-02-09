package org.keycloak.credential;

import java.util.List;

public class CredentialMetadata {
    LocalizedMessage infoMessage;
    List<LocalizedMessage> infoProperties;
    LocalizedMessage warningMessageTitle;
    LocalizedMessage warningMessageDescription;
    CredentialModel credentialModel;

    public CredentialModel getCredentialModel() {
        return credentialModel;
    }

    public void setCredentialModel(CredentialModel credentialModel) {
        this.credentialModel = credentialModel;
    }

    public LocalizedMessage getInfoMessage() {
        return infoMessage;
    }

    public List<LocalizedMessage> getInfoProperties() {
        return infoProperties;
    }

    public LocalizedMessage getWarningMessageTitle() {
        return warningMessageTitle;
    }

    public LocalizedMessage getWarningMessageDescription() {
        return warningMessageDescription;
    }

    public void setWarningMessageTitle(String key, String... parameters) {
        LocalizedMessage message = new LocalizedMessage(key, parameters);
        this.warningMessageTitle = message;
    }

    public void setWarningMessageDescription(String key, String... parameters) {
        LocalizedMessage message = new LocalizedMessage(key, parameters);
        this.warningMessageDescription = message;
    }

    public void setInfoMessage(String key, String... parameters) {
        LocalizedMessage message = new LocalizedMessage(key, parameters);
        this.infoMessage = message;
    }

    public void setInfoProperties(List<LocalizedMessage> infoProperties) {
        this.infoProperties = infoProperties;
    }

    public static class LocalizedMessage {
        private final String key;
        private final Object[] parameters;

        public LocalizedMessage(String key, Object[] parameters) {
            this.key = key;
            this.parameters = parameters;
        }

        public String getKey() {
            return key;
        }

        public Object[] getParameters() {
            return parameters;
        }
    }

}
