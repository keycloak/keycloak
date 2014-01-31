package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.keycloak.entities.CredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around UserData object, which will persist wrapped object after each set operation (compatibility with picketlink based impl)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAdapter implements UserModel {

    private final UserEntity user;
    private final MongoStore mongoStore;

    public UserAdapter(UserEntity userEntity, MongoStore mongoStore) {
        this.user = userEntity;
        this.mongoStore = mongoStore;
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
        updateUser();
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
        updateUser();
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
        updateUser();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        user.setEmail(email);
        updateUser();
    }

    @Override
    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        user.setEmailVerified(verified);
        updateUser();
    }

    @Override
    public void setAttribute(String name, String value) {
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<String, String>());
        }

        user.getAttributes().put(name, value);
        updateUser();
    }

    @Override
    public void removeAttribute(String name) {
        if (user.getAttributes() == null) return;

        user.getAttributes().remove(name);
        updateUser();
    }

    @Override
    public String getAttribute(String name) {
        return user.getAttributes()==null ? null : user.getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return user.getAttributes()==null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(user.getAttributes());
    }

    public UserEntity getUser() {
        return user;
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        if (user.getWebOrigins() != null) {
            result.addAll(user.getWebOrigins());
        }
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        List<String> result = new ArrayList<String>();
        result.addAll(webOrigins);
        user.setWebOrigins(result);
        updateUser();
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        mongoStore.pushItemToList(user, "webOrigins", webOrigin, true);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        mongoStore.pullItemFromList(user, "webOrigins", webOrigin);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        if (user.getRedirectUris() != null) {
            result.addAll(user.getRedirectUris());
        }
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        List<String> result = new ArrayList<String>();
        result.addAll(redirectUris);
        user.setRedirectUris(result);
        updateUser();
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        mongoStore.pushItemToList(user, "redirectUris", redirectUri, true);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        mongoStore.pullItemFromList(user, "redirectUris", redirectUri);
    }

    @Override
    public Set<RequiredAction> getRequiredActions() {
        Set<RequiredAction> result = new HashSet<RequiredAction>();
        if (user.getRequiredActions() != null) {
            result.addAll(user.getRequiredActions());
        }
        return result;
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        mongoStore.pushItemToList(user, "requiredActions", action, true);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        mongoStore.pullItemFromList(user, "requiredActions", action);
    }

    @Override
    public boolean isTotp() {
        return user.isTotp();
    }

    @Override
    public void setTotp(boolean totp) {
        user.setTotp(totp);
        updateUser();
    }

    protected void updateUser() {
        mongoStore.updateObject(user);
    }
}
