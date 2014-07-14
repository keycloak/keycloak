package org.keycloak.admin.client;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class Config {

    private String serverUrl;
    private String realm;
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;

    public Config(String serverUrl, String realm, String username, String password, String clientId, String clientSecret) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isPublicClient(){
        return clientSecret == null;
    }

}
