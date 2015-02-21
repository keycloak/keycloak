package org.keycloak.models.cache.entities;

import org.keycloak.enums.SslRequired;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClaimTypeModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.cache.RealmCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedRealm {

    private String id;
    private String name;
    private boolean enabled;
    private SslRequired sslRequired;
    private boolean registrationAllowed;
    private boolean registrationEmailAsUsername;
    private boolean rememberMe;
    private boolean verifyEmail;
    private boolean passwordCredentialGrantAllowed;
    private boolean resetPasswordAllowed;
    private boolean identityFederationEnabled;
    //--- brute force settings
    private boolean bruteForceProtected;
    private int maxFailureWaitSeconds;
    private int minimumQuickLoginWaitSeconds;
    private int waitIncrementSeconds;
    private long quickLoginCheckMilliSeconds;
    private int maxDeltaTimeSeconds;
    private int failureFactor;
    //--- end brute force settings

    private int ssoSessionIdleTimeout;
    private int ssoSessionMaxLifespan;
    private int accessTokenLifespan;
    private int accessCodeLifespan;
    private int accessCodeLifespanUserAction;
    private int accessCodeLifespanLogin;
    private int notBefore;
    private PasswordPolicy passwordPolicy;

    private String publicKeyPem;
    private String privateKeyPem;
    private String certificatePem;
    private String codeSecret;

    private String loginTheme;
    private String accountTheme;
    private String adminTheme;
    private String emailTheme;
    private String masterAdminApp;

    private List<RequiredCredentialModel> requiredCredentials = new ArrayList<RequiredCredentialModel>();
    private List<UserFederationProviderModel> userFederationProviders = new ArrayList<UserFederationProviderModel>();
    private List<IdentityProviderModel> identityProviders = new ArrayList<IdentityProviderModel>();

    private Map<String, String> browserSecurityHeaders = new HashMap<String, String>();
    private Map<String, String> smtpConfig = new HashMap<String, String>();

    private boolean eventsEnabled;
    private long eventsExpiration;
    private Set<String> eventsListeners = new HashSet<String>();
    private List<String> defaultRoles = new LinkedList<String>();
    private Map<String, String> realmRoles = new HashMap<String, String>();
    private Map<String, String> applications = new HashMap<String, String>();
    private Map<String, String> clients = new HashMap<String, String>();
    private boolean internationalizationEnabled;
    private Set<String> supportedLocales = new HashSet<String>();
    private String defaultLocale;

    public CachedRealm() {
    }

    public CachedRealm(RealmCache cache, RealmProvider delegate, RealmModel model) {
        id = model.getId();
        name = model.getName();
        enabled = model.isEnabled();
        sslRequired = model.getSslRequired();
        registrationAllowed = model.isRegistrationAllowed();
        registrationEmailAsUsername = model.isRegistrationEmailAsUsername();
        rememberMe = model.isRememberMe();
        verifyEmail = model.isVerifyEmail();
        passwordCredentialGrantAllowed = model.isPasswordCredentialGrantAllowed();
        resetPasswordAllowed = model.isResetPasswordAllowed();
        identityFederationEnabled = model.isIdentityFederationEnabled();
        //--- brute force settings
        bruteForceProtected = model.isBruteForceProtected();
        maxFailureWaitSeconds = model.getMaxFailureWaitSeconds();
        minimumQuickLoginWaitSeconds = model.getMinimumQuickLoginWaitSeconds();
        waitIncrementSeconds = model.getWaitIncrementSeconds();
        quickLoginCheckMilliSeconds = model.getQuickLoginCheckMilliSeconds();
        maxDeltaTimeSeconds = model.getMaxDeltaTimeSeconds();
        failureFactor = model.getFailureFactor();
        //--- end brute force settings

        ssoSessionIdleTimeout = model.getSsoSessionIdleTimeout();
        ssoSessionMaxLifespan = model.getSsoSessionMaxLifespan();
        accessTokenLifespan = model.getAccessTokenLifespan();
        accessCodeLifespan = model.getAccessCodeLifespan();
        accessCodeLifespanUserAction = model.getAccessCodeLifespanUserAction();
        accessCodeLifespanLogin = model.getAccessCodeLifespanLogin();
        notBefore = model.getNotBefore();
        passwordPolicy = model.getPasswordPolicy();

        publicKeyPem = model.getPublicKeyPem();
        privateKeyPem = model.getPrivateKeyPem();
        certificatePem = model.getCertificatePem();
        codeSecret = model.getCodeSecret();

        loginTheme = model.getLoginTheme();
        accountTheme = model.getAccountTheme();
        adminTheme = model.getAdminTheme();
        emailTheme = model.getEmailTheme();

        requiredCredentials = model.getRequiredCredentials();
        userFederationProviders = model.getUserFederationProviders();

        this.identityProviders = new ArrayList<IdentityProviderModel>();

        for (IdentityProviderModel identityProviderModel : model.getIdentityProviders()) {
            this.identityProviders.add(new IdentityProviderModel(identityProviderModel));
        }

        smtpConfig.putAll(model.getSmtpConfig());
        browserSecurityHeaders.putAll(model.getBrowserSecurityHeaders());

        eventsEnabled = model.isEventsEnabled();
        eventsExpiration = model.getEventsExpiration();
        eventsListeners.addAll(model.getEventsListeners());
        defaultRoles.addAll(model.getDefaultRoles());
        masterAdminApp = model.getMasterAdminApp().getId();

        for (RoleModel role : model.getRoles()) {
            realmRoles.put(role.getName(), role.getId());
            CachedRole cachedRole = new CachedRealmRole(role, model);
            cache.addCachedRole(cachedRole);
        }

        for (ApplicationModel app : model.getApplications()) {
            applications.put(app.getName(), app.getId());
            CachedApplication cachedApp = new CachedApplication(cache, delegate, model, app);
            cache.addCachedApplication(cachedApp);
        }

        for (OAuthClientModel client : model.getOAuthClients()) {
            clients.put(client.getClientId(), client.getId());
            CachedOAuthClient cachedApp = new CachedOAuthClient(cache, delegate, model, client);
            cache.addCachedOAuthClient(cachedApp);
        }

        internationalizationEnabled = model.isInternationalizationEnabled();
        supportedLocales.addAll(model.getSupportedLocales());
        defaultLocale = model.getDefaultLocale();

    }


    public String getId() {
        return id;
    }

    public String getMasterAdminApp() {
        return masterAdminApp;
    }

    public String getName() {
        return name;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public Map<String, String> getRealmRoles() {
        return realmRoles;
    }

    public Map<String, String> getApplications() {
        return applications;
    }

    public Map<String, String> getClients() {
        return clients;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SslRequired getSslRequired() {
        return sslRequired;
    }

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public boolean isRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public boolean isPasswordCredentialGrantAllowed() {
        return passwordCredentialGrantAllowed;
    }

    public boolean isRememberMe() {
        return this.rememberMe;
    }

    public boolean isBruteForceProtected() {
        return bruteForceProtected;
    }

    public int getMaxFailureWaitSeconds() {
        return this.maxFailureWaitSeconds;
    }

    public int getWaitIncrementSeconds() {
        return this.waitIncrementSeconds;
    }

    public int getMinimumQuickLoginWaitSeconds() {
        return this.minimumQuickLoginWaitSeconds;
    }

    public long getQuickLoginCheckMilliSeconds() {
        return quickLoginCheckMilliSeconds;
    }

    public int getMaxDeltaTimeSeconds() {
        return maxDeltaTimeSeconds;
    }

    public int getFailureFactor() {
        return failureFactor;
    }

    public boolean isVerifyEmail() {
        return verifyEmail;
    }

    public boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public int getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public int getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public int getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public int getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public int getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }
    public int getAccessCodeLifespanLogin() {
        return accessCodeLifespanLogin;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public String getCodeSecret() {
        return codeSecret;
    }

    public List<RequiredCredentialModel> getRequiredCredentials() {
        return requiredCredentials;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public boolean isIdentityFederationEnabled() {
        return identityFederationEnabled;
    }

    public Map<String, String> getSmtpConfig() {
        return smtpConfig;
    }

    public Map<String, String> getBrowserSecurityHeaders() {
        return browserSecurityHeaders;
    }

    public String getLoginTheme() {
        return loginTheme;
    }

    public String getAccountTheme() {
        return accountTheme;
    }

    public String getAdminTheme() {
        return this.adminTheme;
    }

    public String getEmailTheme() {
        return emailTheme;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public boolean isEventsEnabled() {
        return eventsEnabled;
    }

    public long getEventsExpiration() {
        return eventsExpiration;
    }

    public Set<String> getEventsListeners() {
        return eventsListeners;
    }

    public List<UserFederationProviderModel> getUserFederationProviders() {
        return userFederationProviders;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public List<IdentityProviderModel> getIdentityProviders() {
        return identityProviders;
    }

    public boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public Set<String> getSupportedLocales() {
        return supportedLocales;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }
}
