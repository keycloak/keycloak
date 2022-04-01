package org.keycloak.representations.account;

import org.keycloak.representations.idm.CredentialRepresentation;

public class CredentialMetadataRepresentation {

    String infoMessage;
    String warningMessageTitle;
    String warningMessageDescription;

    private CredentialRepresentation credential;


    public CredentialRepresentation getCredential() {
        return credential;
    }

    public void setCredential(CredentialRepresentation credential) {
        this.credential = credential;
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    public void setInfoMessage(String infoMessage) {
        this.infoMessage = infoMessage;
    }

    public String getWarningMessageTitle() {
        return warningMessageTitle;
    }

    public void setWarningMessageTitle(String warningMessageTitle) {
        this.warningMessageTitle = warningMessageTitle;
    }

    public String getWarningMessageDescription() {
        return warningMessageDescription;
    }

    public void setWarningMessageDescription(String warningMessageDescription) {
        this.warningMessageDescription = warningMessageDescription;
    }
}
