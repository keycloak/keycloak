package org.keycloak.testframework.realm;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;

public class ManagedRealmCleanup {

    private final List<RealmCleanup> cleanupTasks = new LinkedList<>();

    /**
     * Add a cleanup task to perform on the realm once the test has completed
     *
     * @param realmCleanup the cleanup to perform on the realm
     * @return
     */
    public ManagedRealmCleanup add(RealmCleanup realmCleanup) {
        this.cleanupTasks.add(realmCleanup);
        return this;
    }

    void resetToOriginalRepresentation(RealmRepresentation rep) {
        if (cleanupTasks.stream().noneMatch(c -> c instanceof ResetRealm)) {
            RealmRepresentation clone = RepresentationUtils.clone(rep);
            cleanupTasks.add(new ResetRealm(clone));
        }
    }

    void runCleanupTasks(RealmResource realm) {
        cleanupTasks.forEach(t -> t.cleanup(realm));
        cleanupTasks.clear();
    }

    public interface RealmCleanup {

        void cleanup(RealmResource realm);

    }

    private record ResetRealm(RealmRepresentation rep) implements RealmCleanup {

        @Override
        public void cleanup(RealmResource realm) {
            realm.update(rep);
        }

    }

}
