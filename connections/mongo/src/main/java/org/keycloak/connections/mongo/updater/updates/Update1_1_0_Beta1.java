package org.keycloak.connections.mongo.updater.updates;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Update1_1_0_Beta1 extends Update {

    @Override
    public String getId() {
        return "1.1.0.Beta1";
    }

    @Override
    public void update() {
        deleteEntries("clientSessions");
        deleteEntries("sessions");
    }

}
