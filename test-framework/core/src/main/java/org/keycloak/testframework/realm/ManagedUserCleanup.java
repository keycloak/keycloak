package org.keycloak.testframework.realm;


import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.LinkedList;
import java.util.List;

public class ManagedUserCleanup {

    private final List<UserCleanup> cleanupTasks = new LinkedList<>();


    public ManagedUserCleanup add(ManagedUserCleanup.UserCleanup userCleanup) {
        this.cleanupTasks.add(userCleanup);
        return this;
    }

    void resetToOriginalRepresentation(UserRepresentation rep) {
        if (cleanupTasks.stream().noneMatch(c -> c instanceof ManagedUserCleanup.ResetUser)) {
            UserRepresentation clone = RepresentationUtils.clone(rep);
            cleanupTasks.add(new ManagedUserCleanup.ResetUser(clone));
        }
    }

    UserRepresentation getOriginalRepresentation() {
        ManagedUserCleanup.ResetUser userCleanup = (ManagedUserCleanup.ResetUser) cleanupTasks.stream().filter(c -> c instanceof ManagedUserCleanup.ResetUser).findFirst().orElse(null);
        return userCleanup != null ? userCleanup.rep() : null;
    }

    void runCleanupTasks(UserResource user) {
        cleanupTasks.forEach(t -> t.cleanup(user));
        cleanupTasks.clear();
    }

    public interface UserCleanup {

        void cleanup(UserResource user);

    }

    private record ResetUser(UserRepresentation rep) implements UserCleanup {

        @Override
        public void cleanup(UserResource user) {
            user.update(rep);
        }
    }
}
