package org.keycloak.testframework.realm;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;

public class ManagedRealmCleanup {

    private final List<RealmCleanup> cleanupTasks = new LinkedList<>();

    public ManagedRealmCleanup add(RealmCleanup realmCleanup) {
        this.cleanupTasks.add(realmCleanup);
        return this;
    }

    public ManagedRealmCleanup deleteUsers() {
        return add(r -> r.users().list().forEach(u -> r.users().delete(u.getId()).close()));
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
