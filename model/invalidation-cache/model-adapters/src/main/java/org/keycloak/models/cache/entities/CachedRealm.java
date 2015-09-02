package org.keycloak.models.cache.entities;

import org.keycloak.enums.SslRequired;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.util.MultivaluedHashMap;

import java.io.Serializable;
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
public class CachedRealm implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private boolean enabled;
    private SslRequired sslRequired;
    private boolean registrationAllowed;
    private boolean registrationEmailAsUsername;
    private boolean rememberMe;
    private boolean verifyEmail;
    private boolean resetPasswordAllowed;
    private boolean identityFederationEnabled;
    private boolean editUsernameAllowed;
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
    private OTPPolicy otpPolicy;

    private String publicKeyPem;
    private String privateKeyPem;
    private String certificatePem;
    private String codeSecret;

    private String loginTheme;
    private String accountTheme;
    private String adminTheme;
    private String emailTheme;
    private String masterAdminClient;

    private List<RequiredCredentialModel> requiredCredentials = new ArrayList<RequiredCredentialModel>();
    private List<UserFederationProviderModel> userFederationProviders = new ArrayList<UserFederationProviderModel>();
    private MultivaluedHashMap<String, UserFederationMapperModel> userFederationMappers = new MultivaluedHashMap<String, UserFederationMapperModel>();
    private List<IdentityProviderModel> identityProviders = new ArrayList<IdentityProviderModel>();

    private Map<String, String> browserSecurityHeaders = new HashMap<String, String>();
    private Map<String, String> smtpConfig = new HashMap<String, String>();
    private Map<String, AuthenticationFlowModel> authenticationFlows = new HashMap<>();
    private Map<String, AuthenticatorConfigModel> authenticatorConfigs = new HashMap<>();
    private Map<String, RequiredActionProviderModel> requiredActionProviders = new HashMap<>();
    private Map<String, RequiredActionProviderModel> requiredActionProvidersByAlias = new HashMap<>();
    private MultivaluedHashMap<String, AuthenticationExecutionModel> authenticationExecutions = new MultivaluedHashMap<>();
    private Map<String, AuthenticationExecutionModel> executionsById = new HashMap<>();

    private AuthenticationFlowModel browserFlow;
    private AuthenticationFlowModel registrationFlow;
    private AuthenticationFlowModel directGrantFlow;
    private AuthenticationFlowModel resetCredentialsFlow;
    private AuthenticationFlowModel clientAuthenticationFlow;

    private boolean eventsEnabled;
    private long eventsExpiration;
    private Set<String> eventsListeners = new HashSet<String>();
    private Set<String> enabledEventTypes = new HashSet<String>();
    protected boolean adminEventsEnabled;
    protected Set<String> adminEnabledEventOperations = new HashSet<String>();
    protected boolean adminEventsDetailsEnabled;
    private List<String> defaultRoles = new LinkedList<String>();
    private Map<String, String> realmRoles = new HashMap<String, String>();
    private Map<String, String> clients = new HashMap<String, String>();
    private boolean internationalizationEnabled;
    private Set<String> supportedLocales = new HashSet<String>();
    private String defaultLocale;
    private MultivaluedHashMap<String, IdentityProviderMapperModel> identityProviderMappers = new MultivaluedHashMap<>();

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
        resetPasswordAllowed = model.isResetPasswordAllowed();
        identityFederationEnabled = model.isIdentityFederationEnabled();
        editUsernameAllowed = model.isEditUsernameAllowed();
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
        otpPolicy = model.getOTPPolicy();

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
        for (UserFederationMapperModel mapper : model.getUserFederationMappers()) {
            userFederationMappers.add(mapper.getFederationProviderId(), mapper);
        }

        this.identityProviders = new ArrayList<>();

        for (IdentityProviderModel identityProviderModel : model.getIdentityProviders()) {
            this.identityProviders.add(new IdentityProviderModel(identityProviderModel));
        }

        for (IdentityProviderMapperModel mapper : model.getIdentityProviderMappers()) {
            identityProviderMappers.add(mapper.getIdentityProviderAlias(), mapper);
        }



        smtpConfig.putAll(model.getSmtpConfig());
        browserSecurityHeaders.putAll(model.getBrowserSecurityHeaders());

        eventsEnabled = model.isEventsEnabled();
        eventsExpiration = model.getEventsExpiration();
        eventsListeners.addAll(model.getEventsListeners());
        enabledEventTypes.addAll(model.getEnabledEventTypes());
        
        adminEventsEnabled = model.isAdminEventsEnabled();
        adminEventsDetailsEnabled = model.isAdminEventsDetailsEnabled();
        
        defaultRoles.addAll(model.getDefaultRoles());
        ClientModel masterAdminClient = model.getMasterAdminClient();
        this.masterAdminClient = (masterAdminClient != null) ? masterAdminClient.getId() : null;

        for (RoleModel role : model.getRoles()) {
            realmRoles.put(role.getName(), role.getId());
            CachedRole cachedRole = new CachedRealmRole(role, model);
            cache.addCachedRole(cachedRole);
        }

        for (ClientModel client : model.getClients()) {
            clients.put(client.getClientId(), client.getId());
            CachedClient cachedClient = new CachedClient(cache, delegate, model, client);
            cache.addCachedClient(cachedClient);
        }

        internationalizationEnabled = model.isInternationalizationEnabled();
        supportedLocales.addAll(model.getSupportedLocales());
        defaultLocale = model.getDefaultLocale();
        for (AuthenticationFlowModel flow : model.getAuthenticationFlows()) {
            authenticationFlows.put(flow.getId(), flow);
            authenticationExecutions.put(flow.getId(), new LinkedList<AuthenticationExecutionModel>());
            for (AuthenticationExecutionModel execution : model.getAuthenticationExecutions(flow.getId())) {
                authenticationExecutions.add(flow.getId(), execution);
                executionsById.put(execution.getId(), execution);
            }
        }
        for (AuthenticatorConfigModel authenticator : model.getAuthenticatorConfigs()) {
            authenticatorConfigs.put(authenticator.getId(), authenticator);
        }
        for (RequiredActionProviderModel action : model.getRequiredActionProviders()) {
            requiredActionProviders.put(action.getId(), action);
            requiredActionProvidersByAlias.put(action.getAlias(), action);
        }

        browserFlow = model.getBrowserFlow();
        registrationFlow = model.getRegistrationFlow();
        directGrantFlow = model.getDirectGrantFlow();
        resetCredentialsFlow = model.getResetCredentialsFlow();
        clientAuthenticationFlow = model.getClientAuthenticationFlow();

    }


    public String getId() {
        return id;
    }

    public String getMasterAdminClient() {
        return masterAdminClient;
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

    public boolean isEditUsernameAllowed() {
        return editUsernameAllowed;
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
    
    public Set<String> getEnabledEventTypes() {
        return enabledEventTypes;
    }

    public boolean isAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public Set<String> getAdminEnabledEventOperations() {
        return adminEnabledEventOperations;
    }

    public boolean isAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public List<UserFederationProviderModel> getUserFederationProviders() {
        return userFederationProviders;
    }

    public MultivaluedHashMap<String, UserFederationMapperModel> getUserFederationMappers() {
        return userFederationMappers;
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

    public MultivaluedHashMap<String, IdentityProviderMapperModel> getIdentityProviderMappers() {
        return identityProviderMappers;
    }

    public Map<String, AuthenticationFlowModel> getAuthenticationFlows() {
        return authenticationFlows;
    }

    public Map<String, AuthenticatorConfigModel> getAuthenticatorConfigs() {
        return authenticatorConfigs;
    }

    public MultivaluedHashMap<String, AuthenticationExecutionModel> getAuthenticationExecutions() {
        return authenticationExecutions;
    }

    public Map<String, AuthenticationExecutionModel> getExecutionsById() {
        return executionsById;
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProviders() {
        return requiredActionProviders;
    }

    public Map<String, RequiredActionProviderModel> getRequiredActionProvidersByAlias() {
        return requiredActionProvidersByAlias;
    }

    public OTPPolicy getOtpPolicy() {
        return otpPolicy;
    }

    public AuthenticationFlowModel getBrowserFlow() {
        return browserFlow;
    }

    public AuthenticationFlowModel getRegistrationFlow() {
        return registrationFlow;
    }

    public AuthenticationFlowModel getDirectGrantFlow() {
        return directGrantFlow;
    }

    public AuthenticationFlowModel getResetCredentialsFlow() {
        return resetCredentialsFlow;
    }

    public AuthenticationFlowModel getClientAuthenticationFlow() {
        return clientAuthenticationFlow;
    }
}
