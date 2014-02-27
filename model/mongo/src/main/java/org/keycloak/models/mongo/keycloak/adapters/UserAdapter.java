package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
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
public class UserAdapter extends AbstractAdapter implements UserModel {

    private final UserEntity user;

    public UserAdapter(UserEntity userEntity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.user = userEntity;
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
    public Set<RequiredAction> getRequiredActions() {
        Set<RequiredAction> result = new HashSet<RequiredAction>();
        if (user.getRequiredActions() != null) {
            result.addAll(user.getRequiredActions());
        }
        return result;
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        getMongoStore().pushItemToList(user, "requiredActions", action, true, invocationContext);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        getMongoStore().pullItemFromList(user, "requiredActions", action, invocationContext);
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
        getMongoStore().updateEntity(user, invocationContext);
    }

    @Override
    public AbstractMongoIdentifiableEntity getMongoEntity() {
        return user;
    }
}
