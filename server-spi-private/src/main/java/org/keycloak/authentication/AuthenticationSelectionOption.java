package org.keycloak.authentication;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;

public class AuthenticationSelectionOption {
    private final AuthenticationExecutionModel authExec;
    private final CredentialModel credential;
    private final AuthenticationFlowModel authFlow;
    private boolean showCredentialName = true;
    private boolean showCredentialType = true;

    public AuthenticationSelectionOption(AuthenticationExecutionModel authExec) {
        this.authExec = authExec;
        this.credential = new CredentialModel();
        this.authFlow = null;
    }

    public AuthenticationSelectionOption(AuthenticationExecutionModel authExec, CredentialModel credential) {
        this.authExec = authExec;
        //Allow themes to get all credential information, but not secret data
        this.credential = credential.shallowClone();
        this.credential.setSecretData("");
        this.authFlow = null;
    }

    public AuthenticationSelectionOption(AuthenticationExecutionModel authExec, AuthenticationFlowModel authFlow) {
        this.authExec = authExec;
        this.credential = new CredentialModel();
        this.authFlow = authFlow;
    }

    public void setShowCredentialName(boolean showCredentialName) {
        this.showCredentialName = showCredentialName;
    }
    public void setShowCredentialType(boolean showCredentialType) {
        this.showCredentialType = showCredentialType;
    }

    public boolean showCredentialName(){
        if (credential.getId() == null) {
            return false;
        }
        return showCredentialName;
    }

    public boolean showCredentialType(){
        return showCredentialType;
    }

    public AuthenticationExecutionModel getAuthenticationExecution() {
        return authExec;
    }

    public String getCredentialId(){
        return credential.getId();
    }

    public String getAuthExecId(){
        return authExec.getId();
    }

    public String getCredentialName() {
        StringBuilder sb = new StringBuilder();
        if (showCredentialName()) {
            if (showCredentialType()) {
                sb.append(" - ");
            }
            if (credential.getUserLabel() == null || credential.getUserLabel().isEmpty()) {
                sb.append(credential.getId());
            } else {
                sb.append(credential.getUserLabel());
            }
        }
        return sb.toString();
    }

    public String getAuthExecName() {
        if (authFlow != null) {
            String authFlowLabel = authFlow.getAlias();
            if (authFlowLabel == null || authFlowLabel.isEmpty()) {
                authFlowLabel = authFlow.getId();
            }
            return authFlowLabel;
        }
        return authExec.getAuthenticator();
    }

    public String getId() {
        if (getCredentialId() == null) {
            return getAuthExecId() + "|";
        }
        return getAuthExecId() + "|" + getCredentialId();
    }

    public CredentialModel getCredential(){
        return credential;
    }
}
