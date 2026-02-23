package org.keycloak.scim.model.user;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;
import org.keycloak.scim.resource.user.User;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.ValidationException;
import org.keycloak.userprofile.ValidationException.Error;

public class UserResourceTypeProvider extends AbstractScimResourceTypeProvider<UserModel, User> {

    public UserResourceTypeProvider(KeycloakSession session) {
        super(session, new UserCoreModelSchema(session), List.of(new UserEnterpriseModelSchema(session)));
    }

    @Override
    public User onCreate(User resource) {
        UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
        UserProfile profile = provider.create(UserProfileContext.SCIM, Map.of(UserModel.USERNAME, resource.getUserName()));
        UserModel model = profile.create(false);

        populate(model, resource);

        try {
            profile = provider.create(UserProfileContext.SCIM, model);
            profile.validate();
        } catch (ValidationException ve) {
            throw handleValidationException(ve);
        }

        return resource;
    }

    @Override
    protected User onUpdate(UserModel model, User resource) {
        try {
            UserProfileProvider userProfileProvider = session.getProvider(UserProfileProvider.class);
            UserProfile profile = userProfileProvider.create(UserProfileContext.SCIM, model);
            profile.update();
        } catch (ValidationException ve) {
            throw handleValidationException(ve);
        }

        return resource;
    }

    @Override
    protected UserModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.users().getUserById(realm, id);
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.USERS_RESOURCE_TYPE;
    }

    @Override
    protected Stream<UserModel> getModels() {
        RealmModel realm = session.getContext().getRealm();
        return session.users().searchForUserStream(realm, Map.of());
    }

    @Override
    public Class<User> getResourceType() {
        return User.class;
    }

    @Override
    public boolean onDelete(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.users().removeUser(realm, getModel(id));
    }

    @Override
    public void close() {

    }

    private ModelValidationException handleValidationException(ValidationException ve) {
        List<Error> errors = ve.getErrors();

        if (errors.isEmpty()) {
            throw new ModelValidationException(ve.getMessage());
        }

        Error firstError = errors.get(0);
        ModelValidationException exception = new ModelValidationException(firstError.getMessage());

        exception.setParameters(firstError.getMessageParameters());

        return exception;
    }
}
