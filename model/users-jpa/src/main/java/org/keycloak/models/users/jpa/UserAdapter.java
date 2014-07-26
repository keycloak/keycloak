package org.keycloak.models.users.jpa;

import org.keycloak.models.UserModel;
import org.keycloak.models.users.Credentials;
import org.keycloak.models.users.User;
import org.keycloak.models.users.jpa.entities.UserAttributeEntity;
import org.keycloak.models.users.jpa.entities.UserCredentialEntity;
import org.keycloak.models.users.jpa.entities.UserEntity;
import org.keycloak.models.users.jpa.entities.UserRoleMappingEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements User {

    protected UserEntity user;
    protected EntityManager em;
    protected String realm;

    public UserAdapter(String realm, EntityManager em, UserEntity user) {
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
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public void setUsername(String username) {
        user.setUsername(username);
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
    public void setAttribute(String name, String value) {
        List<UserAttributeEntity> attributes = user.getAttributes();
        for (UserAttributeEntity a : user.getAttributes()) {
            if (a.getName().equals(name)) {
                a.setValue(value);
                return;
            }
        }
        attributes.add(new UserAttributeEntity(user, name, value));
    }

    @Override
    public String getAttribute(String name) {
        for (UserAttributeEntity a : user.getAttributes()) {
            if (a.getName().equals(name)) {
                return a.getValue();
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> result = new HashMap<String, String>();
        for (UserAttributeEntity a : user.getAttributes()) {
            result.put(a.getName(), a.getValue());
        }
        return result;
    }

    @Override
    public void removeAttribute(String name) {
        Iterator<UserAttributeEntity> itr = user.getAttributes().iterator();
        while(itr.hasNext()) {
            if (itr.next().getName().equals(name)) {
                itr.remove();
                return;
            }
        }
    }

    private UserCredentialEntity getCredentialEntity(String credType) {
        for (UserCredentialEntity entity : user.getCredentials()) {
            if (entity.getType().equals(credType)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public List<Credentials> getCredentials() {
        List<Credentials> result = new ArrayList<Credentials>();
        for (UserCredentialEntity entity : user.getCredentials()) {
            result.add(new Credentials(entity.getType(), entity.getSalt(), entity.getValue(), entity.getHashIterations(), entity.getDevice()));
        }
        return result;
    }

    @Override
    public void updateCredential(Credentials credentials) {
        UserCredentialEntity entity = getCredentialEntity(credentials.getType());
        if (entity == null) {
            entity = new UserCredentialEntity(user, credentials.getType());
            user.getCredentials().add(entity);
        }

        entity.setValue(credentials.getValue());
        entity.setSalt(credentials.getSalt());
        entity.setHashIterations(credentials.getHashIterations());
        entity.setDevice(credentials.getDevice());
    }

    @Override
    public void grantRole(String role) {
        for (UserRoleMappingEntity r : user.getRoles()) {
            if (r.getRole().equals(role)) {
                return;
            }
        }

        user.getRoles().add(new UserRoleMappingEntity(user, role));
    }

    @Override
    public Set<String> getRoleMappings() {
        Set<String> roles = new HashSet<String>();
        for (UserRoleMappingEntity r : user.getRoles()) {
            roles.add(r.getRole());
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(String role) {
        Iterator<UserRoleMappingEntity> itr = user.getRoles().iterator();
        while (itr.hasNext()) {
            if (itr.next().getRole().equals(role)) {
                itr.remove();
                return;
            }
        }
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
