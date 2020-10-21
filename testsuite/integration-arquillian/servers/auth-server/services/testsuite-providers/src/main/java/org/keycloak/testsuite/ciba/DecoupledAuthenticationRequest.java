package org.keycloak.testsuite.ciba;

public class DecoupledAuthenticationRequest {

    private String decoupledAuthId;
    private String userInfo;
    private boolean isConsentRequred;
    private String scope;
    private String defaultClientScope;
    private String bindingMessage;
    private String userCode;

    public String getDecoupledAuthId() {
        return decoupledAuthId;
    }

    public void setDecoupledAuthId(String decoupledAuthId) {
        this.decoupledAuthId = decoupledAuthId;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public boolean isConsentRequired() {
        return isConsentRequred;
    }

    public void setConsentRequired(boolean isConsentRequred) {
        this.isConsentRequred = isConsentRequred;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDefaultClientScope() {
        return defaultClientScope;
    }

    public void setDefaultClientScope(String defaultClientScope) {
        this.defaultClientScope = defaultClientScope;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
}
