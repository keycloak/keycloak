package org.keycloak.services.models.nosql.adapters;

import java.util.Map;

import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.data.UserData;

/**
 * Wrapper around UserData object, which will persist wrapped object after each set operation (compatibility with picketlink based impl)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAdapter implements UserModel {

    private final UserData user;
    private final NoSQL noSQL;

    public UserAdapter(UserData userData, NoSQL noSQL) {
        this.user = userData;
        this.noSQL = noSQL;
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
    public void setEnabled(boolean enabled) {
       user.setEnabled(enabled);
       noSQL.saveObject(user);
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
        noSQL.saveObject(user);
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
        noSQL.saveObject(user);
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        user.setEmail(email);
        noSQL.saveObject(user);
    }

    @Override
    public void setAttribute(String name, String value) {
        user.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        user.removeAttribute(name);
        noSQL.saveObject(user);
    }

    @Override
    public String getAttribute(String name) {
        return user.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return user.getAttributes();
    }
}
