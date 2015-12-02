package org.keycloak.authentication.requiredactions.util;

import java.util.List;
import java.util.Map;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserUpdateProfileContext implements UpdateProfileContext {

    private final RealmModel realm;
    private final UserModel user;

    public UserUpdateProfileContext(RealmModel realm, UserModel user) {
        this.realm = realm;
        this.user = user;
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return realm.isEditUsernameAllowed();
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
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        user.setEmail(email);
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
    public Map<String, List<String>> getAttributes() {
        return user.getAttributes();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        user.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String key, List<String> value) {
        user.setAttribute(key, value);
    }

    @Override
    public String getFirstAttribute(String name) {
        return user.getFirstAttribute(name);
    }

    @Override
    public List<String> getAttribute(String key) {
        return user.getAttribute(key);
    }
}
