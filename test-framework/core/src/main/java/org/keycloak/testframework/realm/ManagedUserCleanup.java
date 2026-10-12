package org.keycloak.testframework.realm;


import java.util.LinkedList;
import java.util.List;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

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

    void runCleanupTasks(UserResource user) {
        CleanupTasks.runAll(cleanupTasks, t -> t.cleanup(user), "user");
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
