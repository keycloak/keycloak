package org.keycloak.representations.idm;

public class TestLdapConnectionRepresentation {

    private String action;
    private String connectionUrl;
    private String bindDn;
    private String bindCredential;
    private String useTruststoreSpi;
    private String connectionTimeout;
    private String componentId;
    private String startTls;
    private String authType;

    public TestLdapConnectionRepresentation() {
    }

    public TestLdapConnectionRepresentation(String action, String connectionUrl, String bindDn, String bindCredential, String useTruststoreSpi, String connectionTimeout) {
        this(action, connectionUrl, bindDn, bindCredential, useTruststoreSpi, connectionTimeout, null, null, null);
    }

    public TestLdapConnectionRepresentation(String action, String connectionUrl, String bindDn, String bindCredential, String useTruststoreSpi, String connectionTimeout, String startTls, String authType) {
        this(action, connectionUrl, bindDn, bindCredential, useTruststoreSpi, connectionTimeout, startTls, authType, null);
    }

    public TestLdapConnectionRepresentation(String action, String connectionUrl, String bindDn, String bindCredential,
            String useTruststoreSpi, String connectionTimeout, String startTls, String authType, String componentId) {
        this.action = action;
        this.connectionUrl = connectionUrl;
        this.bindDn = bindDn;
        this.bindCredential = bindCredential;
        this.useTruststoreSpi = useTruststoreSpi;
        this.connectionTimeout = connectionTimeout;
        this.startTls = startTls;
        this.authType = authType;
        this.componentId = componentId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindCredential() {
        return bindCredential;
    }

    public void setBindCredential(String bindCredential) {
        this.bindCredential = bindCredential;
    }

    public String getUseTruststoreSpi() {
        return useTruststoreSpi;
    }

    public void setUseTruststoreSpi(String useTruststoreSpi) {
        this.useTruststoreSpi = useTruststoreSpi;
    }

    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getStartTls() {
        return startTls;
    }

    public void setStartTls(String startTls) {
        this.startTls = startTls;
    }

}
