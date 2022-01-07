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

package org.keycloak.models.jpa;

import org.keycloak.Config;
import org.jboss.logging.Logger;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.*;
import org.keycloak.models.utils.ComponentUtil;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdapter implements RealmModel, JpaModel<RealmEntity> {
    protected static final Logger logger = Logger.getLogger(RealmAdapter.class);
    protected RealmEntity realm;
    protected EntityManager em;
    protected KeycloakSession session;

    @Override
    public Long getClientsCount() {
        return session.clients().getClientsCount(this);
    }

    private PasswordPolicy passwordPolicy;
    private OTPPolicy otpPolicy;

    public RealmAdapter(KeycloakSession session, EntityManager em, RealmEntity realm) {
        this.session = session;
        this.em = em;
        this.realm = realm;
    }

    public RealmEntity getEntity() {
        return realm;
    }

    @Override
    public String getId() {
        return realm.getId();
    }

    @Override
    public String getName() {
        return realm.getName();
    }

    @Override
    public void setName(String name) {
        realm.setName(name);
        em.flush();
    }

    @Override
    public String getDisplayName() {
        return getAttribute(RealmAttributes.DISPLAY_NAME);
    }

    @Override
    public void setDisplayName(String displayName) {
        setAttribute(RealmAttributes.DISPLAY_NAME, displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        return getAttribute(RealmAttributes.DISPLAY_NAME_HTML);
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        setAttribute(RealmAttributes.DISPLAY_NAME_HTML, displayNameHtml);
    }

    @Override
    public boolean isEnabled() {
        return realm.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        realm.setEnabled(enabled);
        em.flush();
    }

    @Override
    public SslRequired getSslRequired() {
        return realm.getSslRequired() != null ? SslRequired.valueOf(realm.getSslRequired()) : null;
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        realm.setSslRequired(sslRequired.name());
        em.flush();
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        return realm.isAllowUserManagedAccess();
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        realm.setAllowUserManagedAccess(userManagedAccessAllowed);
        em.flush();
    }

    @Override
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
        em.flush();
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        return realm.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        realm.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        if (registrationEmailAsUsername) realm.setDuplicateEmailsAllowed(false);
        em.flush();
    }

    @Override
    public boolean isRememberMe() {
        return realm.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        realm.setRememberMe(rememberMe);
        em.flush();
    }

    @Override
    public void setAttribute(String name, String value) {
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            if (attr.getName().equals(name)) {
                attr.setValue(value);
                return;
            }
        }
        RealmAttributeEntity attr = new RealmAttributeEntity();
        attr.setName(name);
        attr.setValue(value);
        attr.setRealm(realm);
        em.persist(attr);
        realm.getAttributes().add(attr);
    }

    @Override
    public void removeAttribute(String name) {
        Iterator<RealmAttributeEntity> it = realm.getAttributes().iterator();
        while (it.hasNext()) {
            RealmAttributeEntity attr = it.next();
            if (attr.getName().equals(name)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    @Override
    public String getAttribute(String name) {
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            if (attr.getName().equals(name)) {
                return attr.getValue();
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        // should always return a copy
        Map<String, String> result = new HashMap<String, String>();
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            result.put(attr.getName(), attr.getValue());
        }
        return result;
    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        return getAttribute("defaultSignatureAlgorithm");
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        setAttribute("defaultSignatureAlgorithm", defaultSignatureAlgorithm);
    }

    @Override
    public boolean isBruteForceProtected() {
        return getAttribute("bruteForceProtected", false);
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        setAttribute("bruteForceProtected", value);
    }

    @Override
    public boolean isPermanentLockout() {
        return getAttribute("permanentLockout", false);
    }

    @Override
    public void setPermanentLockout(final boolean val) {
        setAttribute("permanentLockout", val);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return getAttribute("maxFailureWaitSeconds", 0);
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        setAttribute("maxFailureWaitSeconds", val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return getAttribute("waitIncrementSeconds", 0);
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        setAttribute("waitIncrementSeconds", val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return getAttribute("quickLoginCheckMilliSeconds", 0L);
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        setAttribute("quickLoginCheckMilliSeconds", val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return getAttribute("minimumQuickLoginWaitSeconds", 0);
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        setAttribute("minimumQuickLoginWaitSeconds", val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return getAttribute("maxDeltaTimeSeconds", 0);
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        setAttribute("maxDeltaTimeSeconds", val);
    }

    @Override
    public int getFailureFactor() {
        return getAttribute("failureFactor", 0);
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        setAttribute("failureFactor", failureFactor);
    }

    @Override
    public boolean isVerifyEmail() {
        return realm.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        realm.setVerifyEmail(verifyEmail);
        em.flush();
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        return realm.isLoginWithEmailAllowed();
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        realm.setLoginWithEmailAllowed(loginWithEmailAllowed);
        if (loginWithEmailAllowed) realm.setDuplicateEmailsAllowed(false);
        em.flush();
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        return realm.isDuplicateEmailsAllowed();
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        realm.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
        if (duplicateEmailsAllowed) {
            realm.setLoginWithEmailAllowed(false);
            realm.setRegistrationEmailAsUsername(false);
        }
        em.flush();
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        realm.setResetPasswordAllowed(resetPasswordAllowed);
        em.flush();
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return realm.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        realm.setEditUsernameAllowed(editUsernameAllowed);
        em.flush();
    }

    @Override
    public int getNotBefore() {
        return realm.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        realm.setNotBefore(notBefore);
    }

    @Override
    public boolean isRevokeRefreshToken() {
        return realm.isRevokeRefreshToken();
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        realm.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        return realm.getRefreshTokenMaxReuse();
    }

    @Override
    public void setRefreshTokenMaxReuse(int revokeRefreshTokenReuseCount) {
        realm.setRefreshTokenMaxReuse(revokeRefreshTokenReuseCount);
    }

    @Override
    public int getAccessTokenLifespan() {
        return realm.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int tokenLifespan) {
        realm.setAccessTokenLifespan(tokenLifespan);
        em.flush();
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        return realm.getAccessTokenLifespanForImplicitFlow();
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        realm.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        return realm.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        realm.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        return realm.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        realm.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        return realm.getSsoSessionIdleTimeoutRememberMe();
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds){
        realm.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        return realm.getSsoSessionMaxLifespanRememberMe();
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        realm.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        return realm.getOfflineSessionIdleTimeout();
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        realm.setOfflineSessionIdleTimeout(seconds);
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
    	return getAttribute(RealmAttributes.OFFLINE_SESSION_MAX_LIFESPAN_ENABLED, false);
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
    	setAttribute(RealmAttributes.OFFLINE_SESSION_MAX_LIFESPAN_ENABLED, offlineSessionMaxLifespanEnabled);
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        return getAttribute(RealmAttributes.OFFLINE_SESSION_MAX_LIFESPAN, Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN);
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        setAttribute(RealmAttributes.OFFLINE_SESSION_MAX_LIFESPAN, seconds);
    }

    @Override
    public int getClientSessionIdleTimeout() {
        return getAttribute(RealmAttributes.CLIENT_SESSION_IDLE_TIMEOUT, 0);
    }

    @Override
    public void setClientSessionIdleTimeout(int seconds) {
        setAttribute(RealmAttributes.CLIENT_SESSION_IDLE_TIMEOUT, seconds);
    }

    @Override
    public int getClientSessionMaxLifespan() {
        return getAttribute(RealmAttributes.CLIENT_SESSION_MAX_LIFESPAN, 0);
    }

    @Override
    public void setClientSessionMaxLifespan(int seconds) {
        setAttribute(RealmAttributes.CLIENT_SESSION_MAX_LIFESPAN, seconds);
    }

    @Override
    public int getClientOfflineSessionIdleTimeout() {
        return getAttribute(RealmAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT, 0);
    }

    @Override
    public void setClientOfflineSessionIdleTimeout(int seconds) {
        setAttribute(RealmAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT, seconds);
    }

    @Override
    public int getClientOfflineSessionMaxLifespan() {
        return getAttribute(RealmAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, 0);
    }

    @Override
    public void setClientOfflineSessionMaxLifespan(int seconds) {
        setAttribute(RealmAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        return realm.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int accessCodeLifespan) {
        realm.setAccessCodeLifespan(accessCodeLifespan);
        em.flush();
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return realm.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        realm.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
        em.flush();
    }

    @Override
    public OAuth2DeviceConfig getOAuth2DeviceConfig() {
        return new OAuth2DeviceConfig(this);
    }

    @Override
    public CibaConfig getCibaPolicy() {
        return new CibaConfig(this);
    }

    @Override
    public ParConfig getParPolicy() {
        return new ParConfig(this);
    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {

        Map<String, Integer> userActionTokens = new HashMap<>();

        getAttributes().entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> nonNull(entry.getValue()))
                .filter(entry -> entry.getKey().startsWith(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "."))
                .forEach(entry -> userActionTokens.put(entry.getKey().substring(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN.length() + 1), Integer.valueOf(entry.getValue())));

        return Collections.unmodifiableMap(userActionTokens);
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        return realm.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int accessCodeLifespanLogin) {
        realm.setAccessCodeLifespanLogin(accessCodeLifespanLogin);
        em.flush();
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        return getAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_ADMIN_LIFESPAN, 12 * 60 * 60);
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int actionTokenGeneratedByAdminLifespan) {
        setAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_ADMIN_LIFESPAN, actionTokenGeneratedByAdminLifespan);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        return getAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int actionTokenGeneratedByUserLifespan) {
        setAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, actionTokenGeneratedByUserLifespan);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenId) {
        if (actionTokenId == null || getAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenId) == null) {
            return getActionTokenGeneratedByUserLifespan();
        }
        return getAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenId, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenId, Integer actionTokenGeneratedByUserLifespan) {
        if (actionTokenGeneratedByUserLifespan != null)
            setAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenId, actionTokenGeneratedByUserLifespan);
    }

    protected RequiredCredentialModel initRequiredCredentialModel(String type) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(type);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + type);
        }
        return model;
    }

    @Override
    public void addRequiredCredential(String type) {
        RequiredCredentialModel model = initRequiredCredentialModel(type);
        addRequiredCredential(model);
        em.flush();
    }

    public void addRequiredCredential(RequiredCredentialModel model) {
        RequiredCredentialEntity entity = new RequiredCredentialEntity();
        entity.setRealm(realm);
        entity.setInput(model.isInput());
        entity.setSecret(model.isSecret());
        entity.setType(model.getType());
        entity.setFormLabel(model.getFormLabel());
        em.persist(entity);
        realm.getRequiredCredentials().add(entity);
        em.flush();
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        Collection<RequiredCredentialEntity> relationships = realm.getRequiredCredentials();
        if (relationships == null) relationships = new ArrayList<RequiredCredentialEntity>();

        Set<String> already = new HashSet<String>();
        List<RequiredCredentialEntity> remove = new ArrayList<RequiredCredentialEntity>();
        for (RequiredCredentialEntity rel : relationships) {
            if (!creds.contains(rel.getType())) {
                remove.add(rel);
            } else {
                already.add(rel.getType());
            }
        }
        for (RequiredCredentialEntity entity : remove) {
            relationships.remove(entity);
            em.remove(entity);
        }
        for (String cred : creds) {
            if (!already.contains(cred)) {
                addRequiredCredential(cred);
            }
        }
        em.flush();
    }


    @Override
    public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
        return realm.getRequiredCredentials().stream().map(this::toRequiredCredentialModel);
    }

    @Override
    @Deprecated
    public Stream<String> getDefaultRolesStream() {
        return getDefaultRole().getCompositesStream().filter(this::isRealmRole).map(RoleModel::getName);
    }

    private boolean isRealmRole(RoleModel role) {
        return ! role.isClientRole();
    }

    @Override
    @Deprecated
    public void addDefaultRole(String name) {
        getDefaultRole().addCompositeRole(getOrAddRoleId(name));
    }

    private RoleModel getOrAddRoleId(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            role = addRole(name);
        }
        return role;
    }

    @Override
    @Deprecated
    public void removeDefaultRoles(String... defaultRoles) {
        for (String defaultRole : defaultRoles) {
            getDefaultRole().removeCompositeRole(getRole(defaultRole));
        }
    }

    @Override
    public Stream<GroupModel> getDefaultGroupsStream() {
        return realm.getDefaultGroupIds().stream().map(this::getGroupById);
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        Collection<String> groupsIds = realm.getDefaultGroupIds();
        if (groupsIds.contains(group.getId())) return;

        groupsIds.add(group.getId());
        em.flush();
    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        Collection<String> groupIds = realm.getDefaultGroupIds();

        if (groupIds.remove(group.getId())) {
            em.flush();
        }
    }

    @Override
    public Stream<ClientModel> getClientsStream() {
        return session.clients().getClientsStream(this);
    }

    @Override
    public Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults) {
        return session.clients().getClientsStream(this, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
        return session.clients().getAlwaysDisplayInConsoleClientsStream(this);
    }

    @Override
    public ClientModel addClient(String name) {
        return session.clients().addClient(this, name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        return session.clients().addClient(this, id, clientId);
    }

    @Override
    public boolean removeClient(String id) {
        if (id == null) return false;
        ClientModel client = getClientById(id);
        if (client == null) return false;
        return session.clients().removeClient(this, id);
    }

    @Override
    public ClientModel getClientById(String id) {
        return session.clients().getClientById(this, id);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return session.clients().getClientByClientId(this, clientId);
    }

    @Override
    public Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByClientIdStream(this, clientId, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByAttributes(this, attributes, firstResult, maxResults);
    }

    private static final String BROWSER_HEADER_PREFIX = "_browser_header.";

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        Map<String, String> attributes = getAttributes();
        if (attributes.isEmpty()) return Collections.EMPTY_MAP;
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().startsWith(BROWSER_HEADER_PREFIX)) {
                headers.put(entry.getKey().substring(BROWSER_HEADER_PREFIX.length()), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            setAttribute(BROWSER_HEADER_PREFIX + entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        Map<String, String> config = new HashMap<String, String>();
        config.putAll(realm.getSmtpConfig());
        return Collections.unmodifiableMap(config);
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        realm.setSmtpConfig(smtpConfig);
        em.flush();
    }


    @Override
    public RoleModel getRole(String name) {
        return session.roles().getRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return session.roles().addRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.roles().addRealmRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return session.roles().removeRole(role);
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        return session.roles().getRealmRolesStream(this);
    }
    
    @Override
    public Stream<RoleModel> getRolesStream(Integer first, Integer max) {
        return session.roles().getRealmRolesStream(this, first, max);
    }
    
    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return session.roles().searchForRolesStream(this, search, first, max);
    }

    @Override
    public RoleModel getRoleById(String id) {
        return session.roles().getRoleById(this, id);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (passwordPolicy == null) {
            passwordPolicy = PasswordPolicy.parse(session, realm.getPasswordPolicy());
        }
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        this.passwordPolicy = policy;
        realm.setPasswordPolicy(policy.toString());
        em.flush();
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        if (otpPolicy == null) {
            otpPolicy = new OTPPolicy();
            otpPolicy.setDigits(realm.getOtpPolicyDigits());
            otpPolicy.setAlgorithm(realm.getOtpPolicyAlgorithm());
            otpPolicy.setInitialCounter(realm.getOtpPolicyInitialCounter());
            otpPolicy.setLookAheadWindow(realm.getOtpPolicyLookAheadWindow());
            otpPolicy.setType(realm.getOtpPolicyType());
            otpPolicy.setPeriod(realm.getOtpPolicyPeriod());
        }
        return otpPolicy;
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        realm.setOtpPolicyAlgorithm(policy.getAlgorithm());
        realm.setOtpPolicyDigits(policy.getDigits());
        realm.setOtpPolicyInitialCounter(policy.getInitialCounter());
        realm.setOtpPolicyLookAheadWindow(policy.getLookAheadWindow());
        realm.setOtpPolicyType(policy.getType());
        realm.setOtpPolicyPeriod(policy.getPeriod());
        em.flush();
    }


    // WebAuthn

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        return getWebAuthnPolicy("");
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        setWebAuthnPolicy(policy, "");
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        // We will use some prefix for attributes related to passwordless WebAuthn policy
        return getWebAuthnPolicy(Constants.WEBAUTHN_PASSWORDLESS_PREFIX);
    }

    @Override
    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        // We will use some prefix for attributes related to passwordless WebAuthn policy
        setWebAuthnPolicy(policy, Constants.WEBAUTHN_PASSWORDLESS_PREFIX);
    }


    private WebAuthnPolicy getWebAuthnPolicy(String attributePrefix) {
        WebAuthnPolicy policy = new WebAuthnPolicy();

        // mandatory parameters
        String rpEntityName = getAttribute(RealmAttributes.WEBAUTHN_POLICY_RP_ENTITY_NAME + attributePrefix);
        if (rpEntityName == null || rpEntityName.isEmpty())
            rpEntityName = Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME;
        policy.setRpEntityName(rpEntityName);

        String signatureAlgorithmsString = getAttribute(RealmAttributes.WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS + attributePrefix);
        if (signatureAlgorithmsString == null || signatureAlgorithmsString.isEmpty())
            signatureAlgorithmsString = Constants.DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS;
        List<String> signatureAlgorithms = Arrays.asList(signatureAlgorithmsString.split(","));
        policy.setSignatureAlgorithm(signatureAlgorithms);

        // optional parameters
        String rpId = getAttribute(RealmAttributes.WEBAUTHN_POLICY_RP_ID + attributePrefix);
        if (rpId == null || rpId.isEmpty()) rpId = "";
        policy.setRpId(rpId);

        String attestationConveyancePreference = getAttribute(RealmAttributes.WEBAUTHN_POLICY_ATTESTATION_CONVEYANCE_PREFERENCE + attributePrefix);
        if (attestationConveyancePreference == null || attestationConveyancePreference.isEmpty())
            attestationConveyancePreference = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        policy.setAttestationConveyancePreference(attestationConveyancePreference);

        String authenticatorAttachment = getAttribute(RealmAttributes.WEBAUTHN_POLICY_AUTHENTICATOR_ATTACHMENT + attributePrefix);
        if (authenticatorAttachment == null || authenticatorAttachment.isEmpty())
            authenticatorAttachment = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        policy.setAuthenticatorAttachment(authenticatorAttachment);

        String requireResidentKey = getAttribute(RealmAttributes.WEBAUTHN_POLICY_REQUIRE_RESIDENT_KEY + attributePrefix);
        if (requireResidentKey == null || requireResidentKey.isEmpty())
            requireResidentKey = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        policy.setRequireResidentKey(requireResidentKey);

        String userVerificationRequirement = getAttribute(RealmAttributes.WEBAUTHN_POLICY_USER_VERIFICATION_REQUIREMENT + attributePrefix);
        if (userVerificationRequirement == null || userVerificationRequirement.isEmpty())
            userVerificationRequirement = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        policy.setUserVerificationRequirement(userVerificationRequirement);

        String createTime = getAttribute(RealmAttributes.WEBAUTHN_POLICY_CREATE_TIMEOUT + attributePrefix);
        if (createTime != null) policy.setCreateTimeout(Integer.parseInt(createTime));
        else policy.setCreateTimeout(0);

        String avoidSameAuthenticatorRegister = getAttribute(RealmAttributes.WEBAUTHN_POLICY_AVOID_SAME_AUTHENTICATOR_REGISTER + attributePrefix);
        if (avoidSameAuthenticatorRegister != null) policy.setAvoidSameAuthenticatorRegister(Boolean.parseBoolean(avoidSameAuthenticatorRegister));

        String acceptableAaguidsString = getAttribute(RealmAttributes.WEBAUTHN_POLICY_ACCEPTABLE_AAGUIDS + attributePrefix);
        List<String> acceptableAaguids = new ArrayList<>();
        if (acceptableAaguidsString != null && !acceptableAaguidsString.isEmpty())
            acceptableAaguids = Arrays.asList(acceptableAaguidsString.split(","));
        policy.setAcceptableAaguids(acceptableAaguids);

        return policy;
    }



    private void setWebAuthnPolicy(WebAuthnPolicy policy, String attributePrefix) {
        // mandatory parameters
        String rpEntityName = policy.getRpEntityName();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_RP_ENTITY_NAME + attributePrefix, rpEntityName);

        List<String> signatureAlgorithms = policy.getSignatureAlgorithm();
        String signatureAlgorithmsString = String.join(",", signatureAlgorithms);
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS + attributePrefix, signatureAlgorithmsString);

        // optional parameters
        String rpId = policy.getRpId();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_RP_ID + attributePrefix, rpId);

        String attestationConveyancePreference = policy.getAttestationConveyancePreference();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_ATTESTATION_CONVEYANCE_PREFERENCE + attributePrefix, attestationConveyancePreference);

        String authenticatorAttachment = policy.getAuthenticatorAttachment();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_AUTHENTICATOR_ATTACHMENT + attributePrefix, authenticatorAttachment);

        String requireResidentKey = policy.getRequireResidentKey();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_REQUIRE_RESIDENT_KEY + attributePrefix, requireResidentKey);

        String userVerificationRequirement = policy.getUserVerificationRequirement();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_USER_VERIFICATION_REQUIREMENT + attributePrefix, userVerificationRequirement);

        int createTime = policy.getCreateTimeout();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_CREATE_TIMEOUT + attributePrefix, Integer.toString(createTime));

        boolean avoidSameAuthenticatorRegister = policy.isAvoidSameAuthenticatorRegister();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_AVOID_SAME_AUTHENTICATOR_REGISTER + attributePrefix, Boolean.toString(avoidSameAuthenticatorRegister));

        List<String> acceptableAaguids = policy.getAcceptableAaguids();
        if (acceptableAaguids != null && !acceptableAaguids.isEmpty()) {
            String acceptableAaguidsString = String.join(",", acceptableAaguids);
            setAttribute(RealmAttributes.WEBAUTHN_POLICY_ACCEPTABLE_AAGUIDS + attributePrefix, acceptableAaguidsString);
        } else {
            removeAttribute(RealmAttributes.WEBAUTHN_POLICY_ACCEPTABLE_AAGUIDS + attributePrefix);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RealmModel)) return false;

        RealmModel that = (RealmModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String getLoginTheme() {
        return realm.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        realm.setLoginTheme(name);
        em.flush();
    }

    @Override
    public String getAccountTheme() {
        return realm.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        realm.setAccountTheme(name);
        em.flush();
    }

    @Override
    public String getAdminTheme() {
        return realm.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        realm.setAdminTheme(name);
        em.flush();
    }

    @Override
    public String getEmailTheme() {
        return realm.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        realm.setEmailTheme(name);
        em.flush();
    }

    @Override
    public boolean isEventsEnabled() {
        return realm.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        realm.setEventsEnabled(enabled);
        em.flush();
    }

    @Override
    public long getEventsExpiration() {
        return realm.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        realm.setEventsExpiration(expiration);
        em.flush();
    }

    @Override
    public Stream<String> getEventsListenersStream() {
        return realm.getEventsListeners().stream();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        realm.setEventsListeners(listeners);
        em.flush();
    }

    @Override
    public Stream<String> getEnabledEventTypesStream() {
        return realm.getEnabledEventTypes().stream();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        realm.setEnabledEventTypes(enabledEventTypes);
        em.flush();
    }

    @Override
    public boolean isAdminEventsEnabled() {
        return realm.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        realm.setAdminEventsEnabled(enabled);
        em.flush();
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        return realm.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        realm.setAdminEventsDetailsEnabled(enabled);
        em.flush();
    }

    @Override
    public ClientModel getMasterAdminClient() {
        String masterAdminClientId = realm.getMasterAdminClient();
        if (masterAdminClientId == null) {
            return null;
        }
        RealmModel masterRealm = getName().equals(Config.getAdminRealm())
          ? this
          : session.realms().getRealm(Config.getAdminRealm());
        return session.clients().getClientById(masterRealm, masterAdminClientId);
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        String appEntityId = client !=null ? em.getReference(ClientEntity.class, client.getId()).getId() : null;
        realm.setMasterAdminClient(appEntityId);
        em.flush();
    }

    @Override
    public void setDefaultRole(RoleModel role) {
        realm.setDefaultRoleId(role.getId());
    }

    @Override
    public RoleModel getDefaultRole() {
        if (realm.getDefaultRoleId() == null) {
            return null;
        }
        return session.roles().getRoleById(this, realm.getDefaultRoleId());
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProvidersStream() {
        return realm.getIdentityProviders().stream().map(this::entityToModel);
    }

    private IdentityProviderModel entityToModel(IdentityProviderEntity entity) {
        IdentityProviderModel identityProviderModel = new IdentityProviderModel();
        identityProviderModel.setProviderId(entity.getProviderId());
        identityProviderModel.setAlias(entity.getAlias());
        identityProviderModel.setDisplayName(entity.getDisplayName());

        identityProviderModel.setInternalId(entity.getInternalId());
        Map<String, String> config = entity.getConfig();
        Map<String, String> copy = new HashMap<>();
        copy.putAll(config);
        identityProviderModel.setConfig(copy);
        identityProviderModel.setEnabled(entity.isEnabled());
        identityProviderModel.setLinkOnly(entity.isLinkOnly());
        identityProviderModel.setTrustEmail(entity.isTrustEmail());
        identityProviderModel.setAuthenticateByDefault(entity.isAuthenticateByDefault());
        identityProviderModel.setFirstBrokerLoginFlowId(entity.getFirstBrokerLoginFlowId());
        identityProviderModel.setPostBrokerLoginFlowId(entity.getPostBrokerLoginFlowId());
        identityProviderModel.setStoreToken(entity.isStoreToken());
        identityProviderModel.setAddReadTokenRoleOnCreate(entity.isAddReadTokenRoleOnCreate());
        return identityProviderModel;
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        return getIdentityProvidersStream()
                .filter(model -> Objects.equals(model.getAlias(), alias))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        IdentityProviderEntity entity = new IdentityProviderEntity();

        if (identityProvider.getInternalId() == null) {
            entity.setInternalId(KeycloakModelUtils.generateId());
        } else {
            entity.setInternalId(identityProvider.getInternalId());
        }
        entity.setAlias(identityProvider.getAlias());
        entity.setDisplayName(identityProvider.getDisplayName());
        entity.setProviderId(identityProvider.getProviderId());
        entity.setEnabled(identityProvider.isEnabled());
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
        entity.setTrustEmail(identityProvider.isTrustEmail());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
        entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
        entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
        entity.setConfig(identityProvider.getConfig());
        entity.setLinkOnly(identityProvider.isLinkOnly());

        realm.addIdentityProvider(entity);

        identityProvider.setInternalId(entity.getInternalId());

        em.persist(entity);
        em.flush();
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        for (IdentityProviderEntity entity : realm.getIdentityProviders()) {
            if (entity.getAlias().equals(alias)) {

                IdentityProviderModel model = entityToModel(entity);
                em.remove(entity);
                em.flush();

                session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderRemovedEvent() {

                    @Override
                    public RealmModel getRealm() {
                        return RealmAdapter.this;
                    }

                    @Override
                    public IdentityProviderModel getRemovedIdentityProvider() {
                        return model;
                    }

                    @Override
                    public KeycloakSession getKeycloakSession() {
                        return session;
                    }
                });

            }
        }
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        for (IdentityProviderEntity entity : this.realm.getIdentityProviders()) {
            if (entity.getInternalId().equals(identityProvider.getInternalId())) {
                entity.setAlias(identityProvider.getAlias());
                entity.setDisplayName(identityProvider.getDisplayName());
                entity.setEnabled(identityProvider.isEnabled());
                entity.setTrustEmail(identityProvider.isTrustEmail());
                entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
                entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
                entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
                entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
                entity.setStoreToken(identityProvider.isStoreToken());
                entity.setConfig(identityProvider.getConfig());
                entity.setLinkOnly(identityProvider.isLinkOnly());
            }
        }

        em.flush();

        session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderUpdatedEvent() {

            @Override
            public RealmModel getRealm() {
                return RealmAdapter.this;
            }

            @Override
            public IdentityProviderModel getUpdatedIdentityProvider() {
                return identityProvider;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        return !this.realm.getIdentityProviders().isEmpty();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        return realm.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        realm.setInternationalizationEnabled(enabled);
        em.flush();
    }

    @Override
    public Stream<String> getSupportedLocalesStream() {
        return realm.getSupportedLocales().stream();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        realm.setSupportedLocales(locales);
        em.flush();
    }

    @Override
    public String getDefaultLocale() {
        return realm.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        realm.setDefaultLocale(locale);
        em.flush();
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
        return realm.getIdentityProviderMappers().stream().map(this::entityToModel);
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
        return realm.getIdentityProviderMappers().stream()
                .filter(e -> Objects.equals(e.getIdentityProviderAlias(), brokerAlias))
                .map(this::entityToModel);
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        if (getIdentityProviderMapperByName(model.getIdentityProviderAlias(), model.getName()) != null) {
            throw new RuntimeException("identity provider mapper name must be unique per identity provider");
        }
        String id = KeycloakModelUtils.generateId();
        IdentityProviderMapperEntity entity = new IdentityProviderMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setRealm(this.realm);
        entity.setConfig(model.getConfig());

        em.persist(entity);
        this.realm.getIdentityProviderMappers().add(entity);
        return entityToModel(entity);
    }

    protected IdentityProviderMapperEntity getIdentityProviderMapperEntity(String id) {
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected IdentityProviderMapperEntity getIdentityProviderMapperEntityByName(String alias, String name) {
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
            if (entity.getIdentityProviderAlias().equals(alias) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        IdentityProviderMapperEntity toDelete = getIdentityProviderMapperEntity(mapping.getId());
        if (toDelete != null) {
            this.realm.getIdentityProviderMappers().remove(toDelete);
            em.remove(toDelete);
        }

    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntity(mapping.getId());
        entity.setIdentityProviderAlias(mapping.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(mapping.getIdentityProviderMapper());
        if (entity.getConfig() == null) {
            entity.setConfig(mapping.getConfig());
        } else {
            entity.getConfig().clear();
            if (mapping.getConfig() != null) {
                entity.getConfig().putAll(mapping.getConfig());
            }
        }
        em.flush();

    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String alias, String name) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntityByName(alias, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected IdentityProviderMapperModel entityToModel(IdentityProviderMapperEntity entity) {
        IdentityProviderMapperModel mapping = new IdentityProviderMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setIdentityProviderAlias(entity.getIdentityProviderAlias());
        mapping.setIdentityProviderMapper(entity.getIdentityProviderMapper());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        String flowId = realm.getBrowserFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        realm.setBrowserFlow(flow.getId());

    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        String flowId = realm.getRegistrationFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        realm.setRegistrationFlow(flow.getId());

    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        String flowId = realm.getDirectGrantFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        realm.setDirectGrantFlow(flow.getId());

    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        String flowId = realm.getResetCredentialsFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        realm.setResetCredentialsFlow(flow.getId());
    }

    public AuthenticationFlowModel getClientAuthenticationFlow() {
        String flowId = realm.getClientAuthenticationFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        realm.setClientAuthenticationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        String flowId = realm.getDockerAuthenticationFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {
        realm.setDockerAuthenticationFlow(flow.getId());
    }

    @Override
    public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
        return realm.getAuthenticationFlows().stream().map(this::entityToModel);
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        return realm.getAuthenticationFlows().stream()
                .filter(flow -> Objects.equals(flow.getAlias(), alias))
                .findFirst()
                .map(this::entityToModel)
                .orElse(null);
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        return getAuthenticatorConfigsStream()
                .filter(config -> Objects.equals(config.getAlias(), alias))
                .findFirst()
                .orElse(null);
    }

    protected AuthenticationFlowModel entityToModel(AuthenticationFlowEntity entity) {
        AuthenticationFlowModel model = new AuthenticationFlowModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        model.setProviderId(entity.getProviderId());
        model.setDescription(entity.getDescription());
        model.setBuiltIn(entity.isBuiltIn());
        model.setTopLevel(entity.isTopLevel());
        return model;
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        AuthenticationFlowEntity entity = getAuthenticationFlowEntity(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        if (KeycloakModelUtils.isFlowUsed(this, model)) {
            throw new ModelException("Cannot remove authentication flow, it is currently in use");
        }
        AuthenticationFlowEntity entity = getAuthenticationFlowEntity(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity entity = getAuthenticationFlowEntity(model.getId(), false);
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setTopLevel(model.isTopLevel());

    }

    private AuthenticationFlowEntity getAuthenticationFlowEntity(String id, boolean readForRemove) {
        AuthenticationFlowEntity entity = readForRemove
                ? em.find(AuthenticationFlowEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(AuthenticationFlowEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity())) return null;
        return entity;
    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity entity = new AuthenticationFlowEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId(): model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setTopLevel(model.isTopLevel());
        entity.setRealm(realm);
        realm.getAuthenticationFlows().add(entity);
        em.persist(entity);
        model.setId(entity.getId());
        return model;
    }

    @Override
    public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
        AuthenticationFlowEntity flow = em.getReference(AuthenticationFlowEntity.class, flowId);

        return flow.getExecutions().stream()
                .filter(e -> getId().equals(e.getRealm().getId()))
                .map(this::entityToModel)
                .sorted(AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
    }

    public AuthenticationExecutionModel entityToModel(AuthenticationExecutionEntity entity) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(entity.getId());
        model.setRequirement(entity.getRequirement());
        model.setPriority(entity.getPriority());
        model.setAuthenticator(entity.getAuthenticator());
        model.setFlowId(entity.getFlowId());
        model.setParentFlow(entity.getParentFlow().getId());
        model.setAuthenticatorFlow(entity.isAutheticatorFlow());
        model.setAuthenticatorConfig(entity.getAuthenticatorConfig());
        return model;
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        AuthenticationExecutionEntity entity = getAuthenticationExecution(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        TypedQuery<AuthenticationExecutionEntity> query = em.createNamedQuery("authenticationFlowExecution", AuthenticationExecutionEntity.class)
                .setParameter("flowId", flowId);
        if (query.getResultList().isEmpty()) {
            return null;
        }
        AuthenticationExecutionEntity authenticationFlowExecution = query.getResultList().get(0);
        return entityToModel(authenticationFlowExecution);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = new AuthenticationExecutionEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId(): model.getId();
        entity.setId(id);
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setFlowId(model.getFlowId());
        entity.setRequirement(model.getRequirement());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        AuthenticationFlowEntity flow = em.find(AuthenticationFlowEntity.class, model.getParentFlow());
        entity.setParentFlow(flow);
        flow.getExecutions().add(entity);
        entity.setRealm(realm);
        entity.setAutheticatorFlow(model.isAuthenticatorFlow());
        em.persist(entity);
        model.setId(entity.getId());
        return model;

    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = getAuthenticationExecution(model.getId(), false);
        if (entity == null) return;
        entity.setAutheticatorFlow(model.isAuthenticatorFlow());
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setRequirement(model.getRequirement());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        entity.setFlowId(model.getFlowId());
        if (model.getParentFlow() != null) {
            AuthenticationFlowEntity flow = em.find(AuthenticationFlowEntity.class, model.getParentFlow());
            entity.setParentFlow(flow);
        }
        em.flush();
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = getAuthenticationExecution(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();

    }

    private AuthenticationExecutionEntity getAuthenticationExecution(String id, boolean readForRemove) {
        AuthenticationExecutionEntity entity = readForRemove
                ? em.find(AuthenticationExecutionEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(AuthenticationExecutionEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity())) return null;
        return entity;
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity auth = new AuthenticatorConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId(): model.getId();
        auth.setId(id);
        auth.setAlias(model.getAlias());
        auth.setRealm(realm);
        auth.setConfig(model.getConfig());
        realm.getAuthenticatorConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();

    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    public AuthenticatorConfigModel entityToModel(AuthenticatorConfigEntity entity) {
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(model.getId(), false);
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();

    }

    private AuthenticatorConfigEntity getAuthenticatorConfigEntity(String id, boolean readForRemove) {
        AuthenticatorConfigEntity entity = readForRemove
                ? em.find(AuthenticatorConfigEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(AuthenticatorConfigEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity())) return null;
        return entity;
    }

    @Override
    public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
        return realm.getAuthenticatorConfigs().stream().map(this::entityToModel);
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity auth = new RequiredActionProviderEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId(): model.getId();
        auth.setId(id);
        auth.setAlias(model.getAlias());
        auth.setName(model.getName());
        auth.setRealm(realm);
        auth.setProviderId(model.getProviderId());
        auth.setConfig(model.getConfig());
        auth.setEnabled(model.isEnabled());
        auth.setDefaultAction(model.isDefaultAction());
        auth.setPriority(model.getPriority());
        realm.getRequiredActionProviders().add(auth);
        em.persist(auth);
        em.flush();
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = getRequiredProviderEntity(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();

    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        RequiredActionProviderEntity entity = getRequiredProviderEntity(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    public RequiredActionProviderModel entityToModel(RequiredActionProviderEntity entity) {
        RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setId(entity.getId());
        model.setProviderId(entity.getProviderId());
        model.setAlias(entity.getAlias());
        model.setEnabled(entity.isEnabled());
        model.setDefaultAction(entity.isDefaultAction());
        model.setPriority(entity.getPriority());
        model.setName(entity.getName());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = getRequiredProviderEntity(model.getId(), false);
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        entity.setProviderId(model.getProviderId());
        entity.setEnabled(model.isEnabled());
        entity.setDefaultAction(model.isDefaultAction());
        entity.setPriority(model.getPriority());
        entity.setName(model.getName());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();

    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        return realm.getRequiredActionProviders().stream()
                .map(this::entityToModel)
                .sorted(RequiredActionProviderModel.RequiredActionComparator.SINGLETON);
    }

    private RequiredActionProviderEntity getRequiredProviderEntity(String id, boolean readForRemove) {
        RequiredActionProviderEntity entity = readForRemove
                ? em.find(RequiredActionProviderEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(RequiredActionProviderEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity())) return null;
        return entity;
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        return getRequiredActionProvidersStream()
                .filter(action -> Objects.equals(action.getAlias(), alias))
                .findFirst()
                .orElse(null);
    }

    @Override
    public GroupModel createGroup(String id, String name, GroupModel toParent) {
        return session.groups().createGroup(this, id, name, toParent);
    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        session.groups().moveGroup(this, group, toParent);
    }

    @Override
    public GroupModel getGroupById(String id) {
        return session.groups().getGroupById(this, id);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return session.groups().getGroupsStream(this);
    }

    @Override
    public Long getGroupsCount(Boolean onlyTopGroups) {
        return session.groups().getGroupsCount(this, onlyTopGroups);
    }

    @Override
    public Long getGroupsCountByNameContaining(String search) {
        return session.groups().getGroupsCountByNameContaining(this, search);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream() {
        return session.groups().getTopLevelGroupsStream(this);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max) {
        return session.groups().getTopLevelGroupsStream(this, first, max);
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(String search, Integer first, Integer max) {
        return session.groups().searchForGroupByNameStream(this, search, first, max);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return session.groups().removeGroup(this, group);
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream() {
        return session.clientScopes().getClientScopesStream(this);
    }

    @Override
    public ClientScopeModel addClientScope(String name) {
        return session.clientScopes().addClientScope(this, name);
    }

    @Override
    public ClientScopeModel addClientScope(String id, String name) {
        return session.clientScopes().addClientScope(this, id, name);
    }

    @Override
    public boolean removeClientScope(String id) {
        return session.clientScopes().removeClientScope(this, id);
    }

    @Override
    public ClientScopeModel getClientScopeById(String id) {
        return session.clientScopes().getClientScopeById(this, id);
    }

    @Override
    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        DefaultClientScopeRealmMappingEntity entity = new DefaultClientScopeRealmMappingEntity();
        entity.setClientScopeId(clientScope.getId());
        entity.setRealm(getEntity());
        entity.setDefaultScope(defaultScope);
        em.persist(entity);
        em.flush();
        em.detach(entity);
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        int numRemoved = em.createNamedQuery("deleteDefaultClientScopeRealmMapping")
                .setParameter("clientScopeId", clientScope.getId())
                .setParameter("realm", getEntity())
                .executeUpdate();
        em.flush();
    }

    @Override
    public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
        TypedQuery<String> query = em.createNamedQuery("defaultClientScopeRealmMappingIdsByRealm", String.class);
        query.setParameter("realm", getEntity());
        query.setParameter("defaultScope", defaultScope);
        return closing(query.getResultStream().map(this::getClientScopeById).filter(Objects::nonNull));
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        model = importComponentModel(model);
        ComponentUtil.notifyCreated(session, this, model);

        return model;
    }

    /**
     * This just exists for testing purposes
     * 
     */
    public static final String COMPONENT_PROVIDER_EXISTS_DISABLED = "component.provider.exists.disabled";

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        ComponentFactory componentFactory = null;
        try {
            componentFactory = ComponentUtil.getComponentFactory(session, model);
            if (componentFactory == null && System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw new IllegalArgumentException("Invalid component type");
            }
            componentFactory.validateConfiguration(session, this, model);
        } catch (Exception e) {
            if (System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw e;
            }

        }


        ComponentEntity c = new ComponentEntity();
        if (model.getId() == null) {
            c.setId(KeycloakModelUtils.generateId());
        } else {
            c.setId(model.getId());
        }
        c.setName(model.getName());
        c.setParentId(model.getParentId());
        if (model.getParentId() == null) {
            c.setParentId(this.getId());
            model.setParentId(this.getId());
        }
        c.setProviderType(model.getProviderType());
        c.setProviderId(model.getProviderId());
        c.setSubType(model.getSubType());
        c.setRealm(realm);
        em.persist(c);
        realm.getComponents().add(c);
        setConfig(model, c);
        model.setId(c.getId());
        return model;
    }

    protected void setConfig(ComponentModel model, ComponentEntity c) {
        c.getComponentConfigs().clear();
        for (String key : model.getConfig().keySet()) {
            List<String> vals = model.getConfig().get(key);
            if (vals == null) {
                continue;
            }
            for (String val : vals) {
                ComponentConfigEntity config = new ComponentConfigEntity();
                config.setId(KeycloakModelUtils.generateId());
                config.setName(key);
                config.setValue(val);
                config.setComponent(c);
                c.getComponentConfigs().add(config);
            }
        }
    }

    @Override
    public void updateComponent(ComponentModel component) {
        ComponentUtil.getComponentFactory(session, component).validateConfiguration(session, this, component);

        ComponentEntity c = getComponentEntity(component.getId());
        if (c == null) return;
        ComponentModel old = entityToModel(c);
        c.setName(component.getName());
        c.setProviderId(component.getProviderId());
        c.setProviderType(component.getProviderType());
        c.setParentId(component.getParentId());
        c.setSubType(component.getSubType());
        setConfig(component, c);
        ComponentUtil.notifyUpdated(session, this, old, component);


    }

    @Override
    public void removeComponent(ComponentModel component) {
        ComponentEntity c = getComponentEntity(component.getId());
        if (c == null) return;
        session.users().preRemove(this, component);
        ComponentUtil.notifyPreRemove(session, this, component);
        removeComponents(component.getId());
        getEntity().getComponents().remove(c);
    }

    @Override
    public void removeComponents(String parentId) {
        Predicate<ComponentEntity> sameParent = c -> Objects.equals(parentId, c.getParentId());

        getEntity().getComponents().stream()
                .filter(sameParent)
                .map(this::entityToModel)
                .forEach((ComponentModel c) -> {
                    session.users().preRemove(this, c);
                    ComponentUtil.notifyPreRemove(session, this, c);
                });

        getEntity().getComponents().removeIf(sameParent);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId, final String providerType) {
        if (parentId == null) parentId = getId();
        final String parent = parentId;

        Stream<ComponentEntity> componentStream = realm.getComponents().stream()
                .filter(c -> Objects.equals(parent, c.getParentId()));

        if (providerType != null) {
            componentStream = componentStream.filter(c -> Objects.equals(providerType, c.getProviderType()));
        }
        return componentStream.map(this::entityToModel);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(final String parentId) {
        return getComponentsStream(parentId, null);
    }

    protected ComponentModel entityToModel(ComponentEntity c) {
        ComponentModel model = new ComponentModel();
        model.setId(c.getId());
        model.setName(c.getName());
        model.setProviderType(c.getProviderType());
        model.setProviderId(c.getProviderId());
        model.setSubType(c.getSubType());
        model.setParentId(c.getParentId());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (ComponentConfigEntity configEntity : c.getComponentConfigs()) {
            config.add(configEntity.getName(), configEntity.getValue());
        }
        model.setConfig(config);
        return model;
    }

    private RequiredCredentialModel toRequiredCredentialModel(RequiredCredentialEntity entity) {
        RequiredCredentialModel model = new RequiredCredentialModel();
        model.setFormLabel(entity.getFormLabel());
        model.setType(entity.getType());
        model.setSecret(entity.isSecret());
        model.setInput(entity.isInput());
        return model;
    }

    @Override
    public Stream<ComponentModel> getComponentsStream() {
        return realm.getComponents().stream().map(this::entityToModel);
    }

    @Override
    public ComponentModel getComponent(String id) {
        ComponentEntity c = getComponentEntity(id);
        return c==null ? null : entityToModel(c);
    }

    private ComponentEntity getComponentEntity(String id) {
        ComponentEntity c = em.find(ComponentEntity.class, id);
        if (c == null) return null;
        if (!c.getRealm().equals(getEntity())) return null;
        return c;
    }

    @Override
    public void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {
        Map<String, RealmLocalizationTextsEntity> currentLocalizationTexts = realm.getRealmLocalizationTexts();
        if(currentLocalizationTexts.containsKey(locale)) {
            RealmLocalizationTextsEntity localizationTextsEntity = currentLocalizationTexts.get(locale);
            Map<String, String> updatedTexts = new HashMap<>(localizationTextsEntity.getTexts());
            updatedTexts.putAll(localizationTexts);
            localizationTextsEntity.setTexts(updatedTexts);

            em.persist(localizationTextsEntity);
        }
        else {
            RealmLocalizationTextsEntity realmLocalizationTextsEntity = new RealmLocalizationTextsEntity();
            realmLocalizationTextsEntity.setRealmId(realm.getId());
            realmLocalizationTextsEntity.setLocale(locale);
            realmLocalizationTextsEntity.setTexts(localizationTexts);

            em.persist(realmLocalizationTextsEntity);
        }
    }

    @Override
    public boolean removeRealmLocalizationTexts(String locale) {
        if (locale == null) return false;
        if (realm.getRealmLocalizationTexts().containsKey(locale))
        {
            em.remove(realm.getRealmLocalizationTexts().get(locale));
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        Map<String, Map<String, String>> localizationTexts = new HashMap<>();
        realm.getRealmLocalizationTexts().forEach((locale, localizationTextsEntity) -> {
            localizationTexts.put(localizationTextsEntity.getLocale(), localizationTextsEntity.getTexts());
        });
        return localizationTexts;
    }

    @Override
    public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
        if (realm.getRealmLocalizationTexts().containsKey(locale)) {
            return realm.getRealmLocalizationTexts().get(locale).getTexts();
        }
        return Collections.emptyMap();
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId());

        ClientInitialAccessEntity entity = new ClientInitialAccessEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealm(realmEntity);

        entity.setCount(count);
        entity.setRemainingCount(count);

        int currentTime = Time.currentTime();
        entity.setTimestamp(currentTime);
        entity.setExpiration(expiration);

        em.persist(entity);

        return entityToModel(entity);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(String id) {
        ClientInitialAccessEntity entity = em.find(ClientInitialAccessEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().getId().equals(realm.getId())) return null;
        return entityToModel(entity);
    }

    @Override
    public void removeClientInitialAccessModel(String id) {
        ClientInitialAccessEntity entity = em.find(ClientInitialAccessEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) return;
        if (!entity.getRealm().getId().equals(realm.getId())) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId());

        TypedQuery<ClientInitialAccessEntity> query = em.createNamedQuery("findClientInitialAccessByRealm", ClientInitialAccessEntity.class);
        query.setParameter("realm", realmEntity);
        return closing(query.getResultStream().map(this::entityToModel));
    }

    @Override
    public void decreaseRemainingCount(ClientInitialAccessModel clientInitialAccess) {
        em.createNamedQuery("decreaseClientInitialAccessRemainingCount")
                .setParameter("id", clientInitialAccess.getId())
                .executeUpdate();
    }

    private ClientInitialAccessModel entityToModel(ClientInitialAccessEntity entity) {
        ClientInitialAccessModel model = new ClientInitialAccessModel();
        model.setId(entity.getId());
        model.setCount(entity.getCount());
        model.setRemainingCount(entity.getRemainingCount());
        model.setExpiration(entity.getExpiration());
        model.setTimestamp(entity.getTimestamp());
        return model;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }
}
