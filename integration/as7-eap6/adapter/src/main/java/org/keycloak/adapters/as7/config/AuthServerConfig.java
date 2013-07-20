package org.keycloak.adapters.as7.config;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthServerConfig {
    @JsonProperty("realm")
    protected String realm;

    @JsonProperty("realm-private-key")
    protected String realmPrivateKey;

    @JsonProperty("realm-public-key")
    protected String realmPublicKey;

    @JsonProperty("realm-keystore")
    protected String realmKeyStore;

    @JsonProperty("realm-keystore-password")
    protected String realmKeystorePassword;

    @JsonProperty("realm-key-alias")
    protected String realmKeyAlias;

    @JsonProperty("realm-private-key-password")
    protected String realmPrivateKeyPassword;

    @JsonProperty("access-code-lifetime")
    protected int accessCodeLifetime;

    @JsonProperty("token-lifetime")
    protected int tokenLifetime;

    @JsonProperty("admin-role")
    protected String adminRole;

    @JsonProperty("login-role")
    protected String loginRole;

    @JsonProperty("oauth-client-role")
    protected String clientRole;

    @JsonProperty("wildcard-role")
    protected String wildcardRole;

    @JsonProperty("cancel-propagation")
    protected boolean cancelPropagation;

    @JsonProperty("sso-disabled")
    protected boolean ssoDisabled;

    // these properties are optional and used to provide connection metadata when the server wants to make
    // remote SSL connections

    protected String truststore;
    @JsonProperty("truststore-password")
    protected String truststorePassword;
    @JsonProperty("client-keystore")
    protected String clientKeystore;
    @JsonProperty("client-keystore-password")
    protected String clientKeystorePassword;
    @JsonProperty("client-key-password")
    protected String clientKeyPassword;

    protected List<String> resources = new ArrayList<String>();


    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealmPrivateKey() {
        return realmPrivateKey;
    }

    public void setRealmPrivateKey(String realmPrivateKey) {
        this.realmPrivateKey = realmPrivateKey;
    }

    public String getRealmPublicKey() {
        return realmPublicKey;
    }

    public void setRealmPublicKey(String realmPublicKey) {
        this.realmPublicKey = realmPublicKey;
    }

    public int getAccessCodeLifetime() {
        return accessCodeLifetime;
    }

    public void setAccessCodeLifetime(int accessCodeLifetime) {
        this.accessCodeLifetime = accessCodeLifetime;
    }

    public String getTruststore() {
        return truststore;
    }

    public void setTruststore(String truststore) {
        this.truststore = truststore;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getClientKeystore() {
        return clientKeystore;
    }

    public void setClientKeystore(String clientKeystore) {
        this.clientKeystore = clientKeystore;
    }

    public String getClientKeystorePassword() {
        return clientKeystorePassword;
    }

    public void setClientKeystorePassword(String clientKeystorePassword) {
        this.clientKeystorePassword = clientKeystorePassword;
    }

    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        this.clientKeyPassword = clientKeyPassword;
    }

    public boolean isCancelPropagation() {
        return cancelPropagation;
    }

    public void setCancelPropagation(boolean cancelPropagation) {
        this.cancelPropagation = cancelPropagation;
    }

    public boolean isSsoDisabled() {
        return ssoDisabled;
    }

    public void setSsoDisabled(boolean ssoDisabled) {
        this.ssoDisabled = ssoDisabled;
    }

    public List<String> getResources() {
        return resources;
    }

    public String getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    public String getLoginRole() {
        return loginRole;
    }

    public void setLoginRole(String loginRole) {
        this.loginRole = loginRole;
    }

    public String getClientRole() {
        return clientRole;
    }

    public void setClientRole(String clientRole) {
        this.clientRole = clientRole;
    }

    public String getWildcardRole() {
        return wildcardRole;
    }

    public void setWildcardRole(String wildcardRole) {
        this.wildcardRole = wildcardRole;
    }

    public String getRealmKeyStore() {
        return realmKeyStore;
    }

    public void setRealmKeyStore(String realmKeyStore) {
        this.realmKeyStore = realmKeyStore;
    }

    public String getRealmKeystorePassword() {
        return realmKeystorePassword;
    }

    public void setRealmKeystorePassword(String realmKeystorePassword) {
        this.realmKeystorePassword = realmKeystorePassword;
    }

    public String getRealmKeyAlias() {
        return realmKeyAlias;
    }

    public void setRealmKeyAlias(String realmKeyAlias) {
        this.realmKeyAlias = realmKeyAlias;
    }

    public String getRealmPrivateKeyPassword() {
        return realmPrivateKeyPassword;
    }

    public void setRealmPrivateKeyPassword(String realmPrivateKeyPassword) {
        this.realmPrivateKeyPassword = realmPrivateKeyPassword;
    }

    public int getTokenLifetime() {
        return tokenLifetime;
    }

    public void setTokenLifetime(int tokenLifetime) {
        this.tokenLifetime = tokenLifetime;
    }
}
