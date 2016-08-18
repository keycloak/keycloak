/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.common.util.MultivaluedHashMap;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
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
public class CachedRealm extends AbstractRevisioned {

    protected String name;
    protected String displayName;
    protected String displayNameHtml;
    protected boolean enabled;
    protected SslRequired sslRequired;
    protected boolean registrationAllowed;
    protected boolean registrationEmailAsUsername;
    protected boolean rememberMe;
    protected boolean verifyEmail;
    protected boolean resetPasswordAllowed;
    protected boolean identityFederationEnabled;
    protected boolean editUsernameAllowed;
    //--- brute force settings
    protected boolean bruteForceProtected;
    protected int maxFailureWaitSeconds;
    protected int minimumQuickLoginWaitSeconds;
    protected int waitIncrementSeconds;
    protected long quickLoginCheckMilliSeconds;
    protected int maxDeltaTimeSeconds;
    protected int failureFactor;
    //--- end brute force settings

    protected boolean revokeRefreshToken;
    protected int ssoSessionIdleTimeout;
    protected int ssoSessionMaxLifespan;
    protected int offlineSessionIdleTimeout;
    protected int accessTokenLifespan;
    protected int accessTokenLifespanForImplicitFlow;
    protected int accessCodeLifespan;
    protected int accessCodeLifespanUserAction;
    protected int accessCodeLifespanLogin;
    protected int notBefore;
    protected PasswordPolicy passwordPolicy;
    protected OTPPolicy otpPolicy;

    protected transient String keyId;
    protected transient PublicKey publicKey;
    protected String publicKeyPem;
    protected transient PrivateKey privateKey;
    protected String privateKeyPem;
    protected transient X509Certificate certificate;
    protected String certificatePem;
    protected String codeSecret;

    protected String loginTheme;
    protected String accountTheme;
    protected String adminTheme;
    protected String emailTheme;
    protected String masterAdminClient;

    protected List<RequiredCredentialModel> requiredCredentials;
    protected List<UserFederationProviderModel> userFederationProviders;
    protected MultivaluedHashMap<String, ComponentModel> componentsByParent = new MultivaluedHashMap<>();
    protected MultivaluedHashMap<String, ComponentModel> componentsByParentAndType = new MultivaluedHashMap<>();
    protected Map<String, ComponentModel> components = new HashMap<>();
    protected MultivaluedHashMap<String, UserFederationMapperModel> userFederationMappers = new MultivaluedHashMap<String, UserFederationMapperModel>();
    protected Set<UserFederationMapperModel> userFederationMapperSet;
    protected List<IdentityProviderModel> identityProviders;

    protected Map<String, String> browserSecurityHeaders;
    protected Map<String, String> smtpConfig;
    protected Map<String, AuthenticationFlowModel> authenticationFlows = new HashMap<>();
    protected List<AuthenticationFlowModel> authenticationFlowList;
    protected Map<String, AuthenticatorConfigModel> authenticatorConfigs = new HashMap<>();
    protected Map<String, RequiredActionProviderModel> requiredActionProviders = new HashMap<>();
    protected List<RequiredActionProviderModel> requiredActionProviderList;
    protected Map<String, RequiredActionProviderModel> requiredActionProvidersByAlias = new HashMap<>();
    protected MultivaluedHashMap<String, AuthenticationExecutionModel> authenticationExecutions = new MultivaluedHashMap<>();
    protected Map<String, AuthenticationExecutionModel> executionsById = new HashMap<>();

    protected AuthenticationFlowModel browserFlow;
    protected AuthenticationFlowModel registrationFlow;
    protected AuthenticationFlowModel directGrantFlow;
    protected AuthenticationFlowModel resetCredentialsFlow;
    protected AuthenticationFlowModel clientAuthenticationFlow;

    protected boolean eventsEnabled;
    protected long eventsExpiration;
    protected Set<String> eventsListeners;
    protected Set<String> enabledEventTypes;
    protected boolean adminEventsEnabled;
    protected Set<String> adminEnabledEventOperations = new HashSet<String>();
    protected boolean adminEventsDetailsEnabled;
    protected List<String> defaultRoles;

    public Set<IdentityProviderMapperModel> getIdentityProviderMapperSet() {
        return identityProviderMapperSet;
    }

    protected List<String> defaultGroups = new LinkedList<String>();
    protected Set<String> groups = new HashSet<String>();
    protected List<String> clientTemplates= new LinkedList<>();
    protected boolean internationalizationEnabled;
    protected Set<String> supportedLocales;
    protected String defaultLocale;
    protected MultivaluedHashMap<String, IdentityProviderMapperModel> identityProviderMappers = new MultivaluedHashMap<>();
    protected Set<IdentityProviderMapperModel> identityProviderMapperSet;

    public CachedRealm(Long revision, RealmModel model) {
        super(revision, model.getId());
        name = model.getName();
        displayName = model.getDisplayName();
        displayNameHtml = model.getDisplayNameHtml();
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

        revokeRefreshToken = model.isRevokeRefreshToken();
        ssoSessionIdleTimeout = model.getSsoSessionIdleTimeout();
        ssoSessionMaxLifespan = model.getSsoSessionMaxLifespan();
        offlineSessionIdleTimeout = model.getOfflineSessionIdleTimeout();
        accessTokenLifespan = model.getAccessTokenLifespan();
        accessTokenLifespanForImplicitFlow = model.getAccessTokenLifespanForImplicitFlow();
        accessCodeLifespan = model.getAccessCodeLifespan();
        accessCodeLifespanUserAction = model.getAccessCodeLifespanUserAction();
        accessCodeLifespanLogin = model.getAccessCodeLifespanLogin();
        notBefore = model.getNotBefore();
        passwordPolicy = model.getPasswordPolicy();
        otpPolicy = model.getOTPPolicy();

        keyId = model.getKeyId();
        publicKeyPem = model.getPublicKeyPem();
        publicKey = model.getPublicKey();
        privateKeyPem = model.getPrivateKeyPem();
        privateKey = model.getPrivateKey();
        certificatePem = model.getCertificatePem();
        certificate = model.getCertificate();
        codeSecret = model.getCodeSecret();

        loginTheme = model.getLoginTheme();
        accountTheme = model.getAccountTheme();
        adminTheme = model.getAdminTheme();
        emailTheme = model.getEmailTheme();

        requiredCredentials = model.getRequiredCredentials();
        userFederationProviders = model.getUserFederationProviders();
        userFederationMapperSet = model.getUserFederationMappers();
        for (UserFederationMapperModel mapper : userFederationMapperSet) {
            this.userFederationMappers.add(mapper.getFederationProviderId(), mapper);
        }

        this.identityProviders = new ArrayList<>();

        for (IdentityProviderModel identityProviderModel : model.getIdentityProviders()) {
            this.identityProviders.add(new IdentityProviderModel(identityProviderModel));
        }
        this.identityProviders = Collections.unmodifiableList(this.identityProviders);

        this.identityProviderMapperSet = model.getIdentityProviderMappers();
        for (IdentityProviderMapperModel mapper : identityProviderMapperSet) {
            identityProviderMappers.add(mapper.getIdentityProviderAlias(), mapper);
        }



        smtpConfig = model.getSmtpConfig();
        browserSecurityHeaders = model.getBrowserSecurityHeaders();

        eventsEnabled = model.isEventsEnabled();
        eventsExpiration = model.getEventsExpiration();
        eventsListeners = model.getEventsListeners();
        enabledEventTypes = model.getEnabledEventTypes();
        
        adminEventsEnabled = model.isAdminEventsEnabled();
        adminEventsDetailsEnabled = model.isAdminEventsDetailsEnabled();
        
        defaultRoles = model.getDefaultRoles();
        ClientModel masterAdminClient = model.getMasterAdminClient();
        this.masterAdminClient = (masterAdminClient != null) ? masterAdminClient.getId() : null;

        cacheClientTemplates(model);

        internationalizationEnabled = model.isInternationalizationEnabled();
        supportedLocales = model.getSupportedLocales();
        defaultLocale = model.getDefaultLocale();
        authenticationFlowList = model.getAuthenticationFlows();
        for (AuthenticationFlowModel flow : authenticationFlowList) {
            this.authenticationFlows.put(flow.getId(), flow);
            authenticationExecutions.put(flow.getId(), new LinkedList<AuthenticationExecutionModel>());
            for (AuthenticationExecutionModel execution : model.getAuthenticationExecutions(flow.getId())) {
                authenticationExecutions.add(flow.getId(), execution);
                executionsById.put(execution.getId(), execution);
            }
        }
        for (GroupModel group : model.getGroups()) {
            groups.add(group.getId());
        }
        for (AuthenticatorConfigModel authenticator : model.getAuthenticatorConfigs()) {
            authenticatorConfigs.put(authenticator.getId(), authenticator);
        }
        requiredActionProviderList = model.getRequiredActionProviders();
        for (RequiredActionProviderModel action : requiredActionProviderList) {
            this.requiredActionProviders.put(action.getId(), action);
            requiredActionProvidersByAlias.put(action.getAlias(), action);
        }

        for (GroupModel group : model.getDefaultGroups()) {
            defaultGroups.add(group.getId());
        }

        browserFlow = model.getBrowserFlow();
        registrationFlow = model.getRegistrationFlow();
        directGrantFlow = model.getDirectGrantFlow();
        resetCredentialsFlow = model.getResetCredentialsFlow();
        clientAuthenticationFlow = model.getClientAuthenticationFlow();

        for (ComponentModel component : model.getComponents()) {
            componentsByParentAndType.add(component.getParentId() + component.getProviderType(), component);
        }
        for (ComponentModel component : model.getComponents()) {
            componentsByParent.add(component.getParentId(), component);
        }
        for (ComponentModel component : model.getComponents()) {
            components.put(component.getId(), component);
        }

    }

    protected void cacheClientTemplates(RealmModel model) {
        for (ClientTemplateModel template : model.getClientTemplates()) {
            clientTemplates.add(template.getId());
        }
    }

    public String getMasterAdminClient() {
        return masterAdminClient;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayNameHtml() {
        return displayNameHtml;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
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

    public boolean isRevokeRefreshToken() {
        return revokeRefreshToken;
    }

    public int getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public int getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public int getOfflineSessionIdleTimeout() {
        return offlineSessionIdleTimeout;
    }

    public int getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public int getAccessTokenLifespanForImplicitFlow() {
        return accessTokenLifespanForImplicitFlow;
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

    public String getKeyId() {
        return keyId;
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

    public Set<String> getGroups() {
        return groups;
    }

    public List<String> getDefaultGroups() {
        return defaultGroups;
    }

    public List<String> getClientTemplates() {
        return clientTemplates;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public Set<UserFederationMapperModel> getUserFederationMapperSet() {
        return userFederationMapperSet;
    }

    public List<AuthenticationFlowModel> getAuthenticationFlowList() {
        return authenticationFlowList;
    }

    public List<RequiredActionProviderModel> getRequiredActionProviderList() {
        return requiredActionProviderList;
    }

    public MultivaluedHashMap<String, ComponentModel> getComponentsByParent() {
        return componentsByParent;
    }

    public MultivaluedHashMap<String, ComponentModel> getComponentsByParentAndType() {
        return componentsByParentAndType;
    }

    public Map<String, ComponentModel> getComponents() {
        return components;
    }
}
