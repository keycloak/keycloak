package org.keycloak.forms.model;

public class RequiredCredential {

    public String type;
    public boolean secret;
    public String formLabel;

    public RequiredCredential(String type, boolean secure, String formLabel) {
        this.type = type;
        this.secret = secure;
        this.formLabel = formLabel;
    }

    public String getName() {
        return type;
    }

    public String getLabel() {
        return formLabel;
    }

    public String getInputType() {
        return secret ? "password" : "text";
    }

}