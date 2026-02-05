package org.keycloak.scim.model.user;

import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;
import org.keycloak.scim.resource.user.User;

public class UserResourceTypeProvider extends AbstractScimResourceTypeProvider<UserModel, User> {

    public UserResourceTypeProvider(KeycloakSession session) {
        super(session, List.of(new UserCoreSchema(session), new UserEnterpriseSchema(session)));
    }

    @Override
    public void onValidate(User resource) throws ModelValidationException {
        RealmModel realm = session.getContext().getRealm();
        String id = resource.getId();

        if (id != null) {
            UserModel model = session.users().getUserById(realm, id);

            if (model != null && !model.getId().equals(id)) {
                throw new ModelValidationException("User with ID " + id + " already exists.");
            }
        }

        if (resource.getEmail() != null) {
            UserProvider users = session.users();
            UserModel model = users.getUserByEmail(realm, resource.getEmail());

            if (model != null && !model.getId().equals(id)) {
                throw new ModelValidationException("User with email " + resource.getEmail() + " already exists.");
            }
        }
    }

    @Override
    public UserModel onCreate(User user) {
        RealmModel realm = session.getContext().getRealm();
        return session.users().addUser(realm, user.getUserName());
    }

    @Override
    protected UserModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.users().getUserById(realm, id);
    }

    @Override
    public boolean delete(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.users().removeUser(realm, getModel(id));
    }

    @Override
    protected User createResourceTypeInstance() {
        return new User();
    }

    @Override
    public void close() {

    }
}
