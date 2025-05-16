package org.keycloak.federation.scim.core;

import static org.keycloak.federation.scim.core.service.AbstractScimService.SCIM_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScimUserStorageProvider implements UserStorageProvider,
        UserRegistrationProvider,
        UserLookupProvider,
        ImportedUserValidation {

    private final KeycloakSession session;
    private final ScimEndPointConfiguration config;
    private final ScimDispatcher dispatcher;
    private final ScimUserStorageTransaction transaction;
    private final Map<String, ScimUserAdapter> managedAdapters = new HashMap<>();

    public ScimUserStorageProvider(KeycloakSession session, ScimEndPointConfiguration config) {
        this.session = session;
        this.config = config;
        this.dispatcher = new ScimDispatcher(session);
        this.transaction = new ScimUserStorageTransaction();
        // TODO: on-prepare not ideal but for now allows to perform operations before flush/commit data to the database
        session.getTransactionManager().enlistPrepare(this.transaction);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        ScimUserAdapter adapter = createAdapter(getUserLocalStorage().addUser(realm, username));
        adapter.setFederationLink(config.getId());
        this.transaction.execute(addUser(adapter));
        return adapter;
    }

    private Runnable addUser(ScimUserAdapter adapter) {
        return () -> {
            dispatcher.dispatchUserModificationToAll(client -> client.create(adapter));
            adapter.getGroupsStream()
                    .forEach(group -> dispatcher.dispatchGroupModificationToAll(client -> client.update(group)));
        };
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        UserModel scimUser = getUserById(realm, user.getId());

        if (scimUser == null) {
            return false;
        }

        removeUser(createAdapter(scimUser));

        return true;
    }

    private void removeUser(ScimUserAdapter adapter) {
        this.transaction.execute(() -> dispatcher.dispatchUserModificationToAll(client -> client.delete(adapter.getFirstAttribute(SCIM_ID))));
    }

    private Runnable updateUser(ScimUserAdapter adapter) {
        return () -> {
            if (adapter.isDirty()) {
                dispatcher.dispatchUserModificationToAll(client -> client.update(adapter));
            }
        };
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        return getByAttribute(UserModel.ID, id);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return getByAttribute(UserModel.USERNAME, username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return getByAttribute(UserModel.EMAIL, email);
    }

    @Override
    public void close() {
        dispatcher.close();
    }

    private UserProvider getUserLocalStorage() {
        return UserStoragePrivateUtil.userLocalStorage(session);
    }

    private ScimUserAdapter getByAttribute(String name, String value) {
        UserModel user = null;
        UserProvider localStorage = getUserLocalStorage();

        switch (name) {
            case UserModel.ID -> user = localStorage.getUserById(session.getContext().getRealm(), value);
            case UserModel.USERNAME -> user = localStorage.getUserByUsername(session.getContext().getRealm(), value);
            case UserModel.EMAIL -> user = localStorage.getUserByEmail(session.getContext().getRealm(), value);
        }

        if (user == null || user.getFirstAttribute(SCIM_ID) == null) {
            return null;
        }

        return createAdapter(user);
    }

    @Override
    public UserModel validate(RealmModel realm, UserModel user) {
        return createAdapter(user);
    }

    private ScimUserAdapter createAdapter(UserModel delegate) {
        return managedAdapters.computeIfAbsent(delegate.getId(), k -> {
            ScimUserAdapter adapter = new ScimUserAdapter(delegate);
            if (delegate.getFirstAttribute(SCIM_ID) != null) {
                transaction.execute(updateUser(adapter));
            }
            return adapter;
        });
    }
}
