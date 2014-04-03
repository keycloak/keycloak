package org.keycloak.models.jpa;

import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements UserModel {

    protected UserEntity user;

    public UserAdapter(UserEntity user) {
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



}
