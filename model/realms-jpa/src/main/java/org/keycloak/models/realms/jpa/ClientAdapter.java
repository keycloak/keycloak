package org.keycloak.models.realms.jpa;

import org.keycloak.models.realms.Client;
import org.keycloak.models.realms.Realm;
import org.keycloak.models.realms.RealmProvider;
import org.keycloak.models.realms.Role;
import org.keycloak.models.realms.RoleContainer;
import org.keycloak.models.realms.jpa.entities.ClientEntity;
import org.keycloak.models.realms.jpa.entities.ScopeMappingEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class ClientAdapter implements Client {
    protected RealmProvider provider;
    protected ClientEntity entity;
    protected EntityManager em;

    public ClientAdapter(RealmProvider provider, ClientEntity entity, EntityManager em) {
        this.provider = provider;
        this.entity = entity;
        this.em = em;
    }

    public ClientEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public Realm getRealm() {
        return new RealmAdapter(provider, em, entity.getRealm());
    }

    @Override
    public String getClientId() {
        return entity.getName();
    }

    @Override
    public boolean isEnabled() {
        return entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public long getAllowedClaimsMask() {
        return entity.getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        entity.setAllowedClaimsMask(mask);
    }

    @Override
    public boolean isPublicClient() {
        return entity.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        entity.setPublicClient(flag);
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        result.addAll(entity.getWebOrigins());
        return result;
    }



    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        entity.setWebOrigins(webOrigins);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        entity.getWebOrigins().add(webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        entity.getWebOrigins().remove(webOrigin);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        result.addAll(entity.getRedirectUris());
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        entity.setRedirectUris(redirectUris);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        entity.getRedirectUris().add(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        entity.getRedirectUris().remove(redirectUri);
    }

    @Override
    public String getSecret() {
        return entity.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        entity.setSecret(secret);
    }

    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(entity.getSecret());
    }

    @Override
    public int getNotBefore() {
        return entity.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        entity.setNotBefore(notBefore);
    }

    @Override
    public Set<Role> getRealmScopeMappings() {
        Set<Role> roleMappings = getScopeMappings();

        Set<Role> appRoles = new HashSet<Role>();
        for (Role role : roleMappings) {
            RoleContainer container = role.getContainer();
            if (container instanceof Realm) {
                if (((Realm) container).getId().equals(entity.getRealm().getId())) {
                    appRoles.add(role);
                }
            }
        }

        return appRoles;
    }



    @Override
    public Set<Role> getScopeMappings() {
        TypedQuery<String> query = em.createNamedQuery("clientScopeMappingIds", String.class);
        query.setParameter("client", getEntity());
        List<String> ids = query.getResultList();
        Set<Role> roles = new HashSet<Role>();
        for (String roleId : ids) {
            Role role = provider.getRoleById(roleId, entity.getRealm().getId());
            if (role == null) continue;
            roles.add(role);
        }
        return roles;
    }

    @Override
    public void addScopeMapping(Role role) {
        ScopeMappingEntity entity = new ScopeMappingEntity();
        entity.setClient(getEntity());
        entity.setRole(((RoleAdapter) role).getRole());
        em.persist(entity);
        em.flush();
        em.detach(entity);
    }

    @Override
    public void deleteScopeMapping(Role role) {
        TypedQuery<ScopeMappingEntity> query = getRealmScopeMappingQuery((RoleAdapter) role);
        List<ScopeMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (ScopeMappingEntity entity : results) {
            em.remove(entity);
        }
    }

    protected TypedQuery<ScopeMappingEntity> getRealmScopeMappingQuery(RoleAdapter role) {
        TypedQuery<ScopeMappingEntity> query = em.createNamedQuery("hasScope", ScopeMappingEntity.class);
        query.setParameter("client", getEntity());
        query.setParameter("role", role.getRole());
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!this.getClass().equals(o.getClass())) return false;

        ClientAdapter that = (ClientAdapter) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return entity.getId().hashCode();
    }
}
