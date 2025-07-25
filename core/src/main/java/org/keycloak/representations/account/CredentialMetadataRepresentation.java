package org.keycloak.representations.account;

import org.keycloak.representations.idm.CredentialRepresentation;

public class CredentialMetadataRepresentation {

    LocalizedMessage infoMessage;
    LocalizedMessage warningMessageTitle;
    LocalizedMessage warningMessageDescription;

    private CredentialRepresentation credential;


    public CredentialRepresentation getCredential() {
        return credential;
    }

    public void setCredential(CredentialRepresentation credential) {
        this.credential = credential;
    }

    public LocalizedMessage getInfoMessage() {
        return infoMessage;
    }

    public void setInfoMessage(LocalizedMessage infoMessage) {
        this.infoMessage = infoMessage;
    }

    public LocalizedMessage getWarningMessageTitle() {
        return warningMessageTitle;
    }

    public void setWarningMessageTitle(LocalizedMessage warningMessageTitle) {
        this.warningMessageTitle = warningMessageTitle;
    }

    public LocalizedMessage getWarningMessageDescription() {
        return warningMessageDescription;
    }

    public void setWarningMessageDescription(LocalizedMessage warningMessageDescription) {
        this.warningMessageDescription = warningMessageDescription;
    }
}
