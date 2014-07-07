package org.keycloak.models.jpa;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.AuthenticationLinkEntity;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.utils.Pbkdf2PasswordEncoder.getSalt;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements UserModel {

    protected UserEntity user;
    protected EntityManager em;
    protected RealmModel realm;

    public UserAdapter(RealmModel realm, EntityManager em, UserEntity user) {
        this.em = em;
        this.user = user;
        this.realm = realm;
    }

    public UserEntity getUser() {
        return user;
    }

    @Override
    public String getId() {
        return user.getId();
    }

    @Override
    public String getLoginName() {
        return user.getLoginName();
    }

    @Override
    public void setLoginName(String loginName) {
        user.setLoginName(loginName);
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public boolean isTotp() {
        return user.isTotp();
    }

    @Override
    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
    }

    @Override
    public void setAttribute(String name, String value) {
        Map<String, String> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<String, String>();
        }
        attributes.put(name, value);
        user.setAttributes(attributes);
    }

    @Override
    public void removeAttribute(String name) {
        Map<String, String> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<String, String>();
        }
        attributes.remove(name);
        user.setAttributes(attributes);
    }

    @Override
    public String getAttribute(String name) {
        if (user.getAttributes() == null) return null;
        return user.getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(user.getAttributes());
        return result;
    }

    @Override
    public Set<RequiredAction> getRequiredActions() {
        Set<RequiredAction> result = new HashSet<RequiredAction>();
        result.addAll(user.getRequiredActions());
        return result;
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        user.getRequiredActions().add(action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        user.getRequiredActions().remove(action);
    }


    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        user.setEmail(email);
    }

    @Override
    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        user.setEmailVerified(verified);
    }

    @Override
    public void setTotp(boolean totp) {
        user.setTotp(totp);
    }

    @Override
    public int getNotBefore() {
        return user.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        user.setNotBefore(notBefore);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        CredentialEntity credentialEntity = getCredentialEntity(user, cred.getType());

        if (credentialEntity == null) {
            credentialEntity = new CredentialEntity();
            credentialEntity.setType(cred.getType());
            credentialEntity.setDevice(cred.getDevice());
            credentialEntity.setUser(user);
            em.persist(credentialEntity);
            user.getCredentials().add(credentialEntity);
        }
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            byte[] salt = getSalt();
            int hashIterations = 1;
            PasswordPolicy policy = realm.getPasswordPolicy();
            if (policy != null) {
                hashIterations = policy.getHashIterations();
                if (hashIterations == -1) hashIterations = 1;
            }
            credentialEntity.setValue(new Pbkdf2PasswordEncoder(salt).encode(cred.getValue(), hashIterations));
            credentialEntity.setSalt(salt);
            credentialEntity.setHashIterations(hashIterations);
        } else {
            credentialEntity.setValue(cred.getValue());
        }
        credentialEntity.setDevice(cred.getDevice());
        em.flush();
    }

    private CredentialEntity getCredentialEntity(UserEntity userEntity, String credType) {
        for (CredentialEntity entity : userEntity.getCredentials()) {
            if (entity.getType().equals(credType)) {
                return entity;
            }
        }

        return null;
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        List<CredentialEntity> credentials = new ArrayList<CredentialEntity>(user.getCredentials());
        List<UserCredentialValueModel> result = new ArrayList<UserCredentialValueModel>();

        if (credentials != null) {
            for (CredentialEntity credEntity : credentials) {
                UserCredentialValueModel credModel = new UserCredentialValueModel();
                credModel.setType(credEntity.getType());
                credModel.setDevice(credEntity.getDevice());
                credModel.setValue(credEntity.getValue());
                credModel.setSalt(credEntity.getSalt());
                credModel.setHashIterations(credEntity.getHashIterations());

                result.add(credModel);
            }
        }

        return result;
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel credModel) {
        CredentialEntity credentialEntity = getCredentialEntity(user, credModel.getType());

        if (credentialEntity == null) {
            credentialEntity = new CredentialEntity();
            credentialEntity.setType(credModel.getType());
            credentialEntity.setUser(user);
            em.persist(credentialEntity);
            user.getCredentials().add(credentialEntity);
        }

        credentialEntity.setValue(credModel.getValue());
        credentialEntity.setSalt(credModel.getSalt());
        credentialEntity.setDevice(credModel.getDevice());
        credentialEntity.setHashIterations(credModel.getHashIterations());

        em.flush();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    protected TypedQuery<UserRoleMappingEntity> getUserRoleMappingEntityTypedQuery(RoleModel role) {
        TypedQuery<UserRoleMappingEntity> query = em.createNamedQuery("userHasRole", UserRoleMappingEntity.class);
        query.setParameter("user", getUser());
        RoleEntity roleEntity = em.getReference(RoleEntity.class, role.getId());
        query.setParameter("role", roleEntity);
        return query;
    }

    @Override
    public void grantRole(RoleModel role) {
        if (hasRole(role)) return;
        UserRoleMappingEntity entity = new UserRoleMappingEntity();
        entity.setUser(getUser());
        RoleEntity roleEntity = em.getReference(RoleEntity.class, role.getId());
        entity.setRole(roleEntity);
        em.persist(entity);
        em.flush();
        em.detach(entity);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> roleMappings = getRoleMappings();

        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }


    @Override
    public Set<RoleModel> getRoleMappings() {
        // we query ids only as the role might be cached and following the @ManyToOne will result in a load
        // even if we're getting just the id.
        TypedQuery<String> query = em.createNamedQuery("userRoleMappingIds", String.class);
        query.setParameter("user", getUser());
        List<String> ids = query.getResultList();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String roleId : ids) {
            RoleModel roleById = realm.getRoleById(roleId);
            if (roleById == null) continue;
            roles.add(roleById);
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (user == null || role == null) return;

        TypedQuery<UserRoleMappingEntity> query = getUserRoleMappingEntityTypedQuery(role);
        List<UserRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (UserRoleMappingEntity entity : results) {
            em.remove(entity);
        }
        em.flush();
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(ApplicationModel app) {
        Set<RoleModel> roleMappings = getRoleMappings();

        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ApplicationModel) {
                ApplicationModel appModel = (ApplicationModel)container;
                if (appModel.getId().equals(app.getId())) {
                   roles.add(role);
                }
            }
        }
        return roles;
    }

    @Override
    public AuthenticationLinkModel getAuthenticationLink() {
        AuthenticationLinkEntity authLinkEntity = user.getAuthenticationLink();
        return authLinkEntity == null ? null : new AuthenticationLinkModel(authLinkEntity.getAuthProvider(), authLinkEntity.getAuthUserId());
    }

    @Override
    public void setAuthenticationLink(AuthenticationLinkModel authenticationLink) {
        AuthenticationLinkEntity entity = new AuthenticationLinkEntity();
        entity.setAuthProvider(authenticationLink.getAuthProvider());
        entity.setAuthUserId(authenticationLink.getAuthUserId());

        user.setAuthenticationLink(entity);
        em.persist(entity);
        em.persist(user);
        em.flush();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserModel)) return false;

        UserModel that = (UserModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }



}
