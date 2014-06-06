package org.keycloak.models.jpa;

import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;

import javax.persistence.EntityManager;
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

    public UserAdapter(EntityManager em, UserEntity user) {
        this.em = em;
        this.user = user;
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
            credentialEntity.setValue(new Pbkdf2PasswordEncoder(salt).encode(cred.getValue()));
            credentialEntity.setSalt(salt);
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

        em.flush();
    }



}
