package org.keycloak.models.cache.infinispan;

import org.keycloak.models.*;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.models.cache.entities.CachedUser;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements UserModel {
    protected UserModel updated;
    protected CachedUser cached;
    protected CacheUserProvider userProviderCache;
    protected KeycloakSession keycloakSession;
    protected RealmModel realm;

    public UserAdapter(CachedUser cached, CacheUserProvider userProvider, KeycloakSession keycloakSession, RealmModel realm) {
        this.cached = cached;
        this.userProviderCache = userProvider;
        this.keycloakSession = keycloakSession;
        this.realm = realm;
    }

    protected void getDelegateForUpdate() {
        if (updated == null) {
            userProviderCache.registerUserInvalidation(realm, getId());
            updated = userProviderCache.getDelegate().getUserById(getId(), realm);
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }
    @Override
    public String getId() {
        if (updated != null) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getUsername() {
        if (updated != null) return updated.getUsername();
        return cached.getUsername();
    }

    @Override
    public void setUsername(String username) {
        getDelegateForUpdate();
        username = KeycloakModelUtils.toLowerCaseSafe(username);
        updated.setUsername(username);
    }

    @Override
    public Long getCreatedTimestamp() {
        // get from cached always as it is immutable
        return cached.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        // nothing to do as this value is immutable
    }

    @Override
    public boolean isEnabled() {
        if (updated != null) return updated.isEnabled();
        return cached.isEnabled();
    }

    @Override
    public boolean isOtpEnabled() {
        if (updated != null) return updated.isOtpEnabled();
        return cached.isTotp();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEnabled(enabled);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        getDelegateForUpdate();
        updated.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getDelegateForUpdate();
        updated.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updated.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (updated != null) return updated.getFirstAttribute(name);
        return cached.getAttributes().getFirst(name);
    }

    @Override
    public List<String> getAttribute(String name) {
        if (updated != null) return updated.getAttribute(name);
        List<String> result = cached.getAttributes().get(name);
        return (result == null) ? Collections.<String>emptyList() : result;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (updated != null) return updated.getAttributes();
        return cached.getAttributes();
    }

    @Override
    public Set<String> getRequiredActions() {
        if (updated != null) return updated.getRequiredActions();
        return cached.getRequiredActions();
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        getDelegateForUpdate();
        updated.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        getDelegateForUpdate();
        updated.removeRequiredAction(action);
    }

    @Override
    public void addRequiredAction(String action) {
        getDelegateForUpdate();
        updated.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(String action) {
        getDelegateForUpdate();
        updated.removeRequiredAction(action);
    }

    @Override
    public String getFirstName() {
        if (updated != null) return updated.getFirstName();
        return cached.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        getDelegateForUpdate();
        updated.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        if (updated != null) return updated.getLastName();
        return cached.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        getDelegateForUpdate();
        updated.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        if (updated != null) return updated.getEmail();
        return cached.getEmail();
    }

    @Override
    public void setEmail(String email) {
        getDelegateForUpdate();
        email = KeycloakModelUtils.toLowerCaseSafe(email);
        updated.setEmail(email);
    }

    @Override
    public boolean isEmailVerified() {
        if (updated != null) return updated.isEmailVerified();
        return cached.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        getDelegateForUpdate();
        updated.setEmailVerified(verified);
    }

    @Override
    public void setOtpEnabled(boolean totp) {
        getDelegateForUpdate();
        updated.setOtpEnabled(totp);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        getDelegateForUpdate();
        updated.updateCredential(cred);
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        if (updated != null) return updated.getCredentialsDirectly();
        return cached.getCredentials();
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        getDelegateForUpdate();
        updated.updateCredentialDirectly(cred);
    }

    @Override
    public String getFederationLink() {
        if (updated != null) return updated.getFederationLink();
        return cached.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        getDelegateForUpdate();
        updated.setFederationLink(link);
   }

    @Override
    public String getServiceAccountClientLink() {
        if (updated != null) return updated.getServiceAccountClientLink();
        return cached.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        getDelegateForUpdate();
        updated.setServiceAccountClientLink(clientInternalId);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        if (updated != null) return updated.getRealmRoleMappings();
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> realmMappings = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                if (((RealmModel) container).getId().equals(realm.getId())) {
                    realmMappings.add(role);
                }
            }
        }
        return realmMappings;
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        if (updated != null) return updated.getClientRoleMappings(app);
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> appMappings = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ClientModel) {
                if (((ClientModel) container).getId().equals(app.getId())) {
                    appMappings.add(role);
                }
            }
        }
        return appMappings;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (updated != null) return updated.hasRole(role);
        if (cached.getRoleMappings().contains(role.getId())) return true;

        Set<RoleModel> mappings = getRoleMappings();
        for (RoleModel mapping: mappings) {
           if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {
        getDelegateForUpdate();
        updated.grantRole(role);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        if (updated != null) return updated.getRoleMappings();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String id : cached.getRoleMappings()) {
            RoleModel roleById = keycloakSession.realms().getRoleById(id, realm);
            if (roleById == null) {
                // chance that role was removed, so just delete to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getRoleMappings();
            }
            roles.add(roleById);

        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        getDelegateForUpdate();
        updated.deleteRoleMapping(role);
    }

    @Override
    public void addConsent(UserConsentModel consent) {
        getDelegateForUpdate();
        updated.addConsent(consent);
    }

    @Override
    public UserConsentModel getConsentByClient(String clientId) {
        // TODO: caching?
        getDelegateForUpdate();
        return updated.getConsentByClient(clientId);
    }

    @Override
    public List<UserConsentModel> getConsents() {
        // TODO: caching?
        getDelegateForUpdate();
        return updated.getConsents();
    }

    @Override
    public void updateConsent(UserConsentModel consent) {
        getDelegateForUpdate();
        updated.updateConsent(consent);
    }

    @Override
    public boolean revokeConsentForClient(String clientId) {
        getDelegateForUpdate();
        return updated.revokeConsentForClient(clientId);
    }
}
