package org.keycloak.scim.model.user;

import java.util.List;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;

public class GroupResourceTypeProvider extends AbstractScimResourceTypeProvider<GroupModel, Group> {

    public GroupResourceTypeProvider(KeycloakSession session) {
        super(session, List.of(new GroupCoreSchema()));
    }

    @Override
    public void onValidate(Group resource) throws ModelValidationException {
    }

    @Override
    public GroupModel onCreate(Group group) {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().createGroup(realm, group.getDisplayName());
    }

    @Override
    protected GroupModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().getGroupById(realm, id);
    }

    @Override
    public boolean delete(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().removeGroup(realm, getModel(id));
    }

    @Override
    protected Group createResourceTypeInstance() {
        return new Group();
    }

    @Override
    public void close() {

    }
}
