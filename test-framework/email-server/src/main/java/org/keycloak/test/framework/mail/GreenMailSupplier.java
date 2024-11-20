package org.keycloak.test.framework.mail;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.mail.annotations.InjectMailServer;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.realm.ManagedRealm;

import java.util.HashMap;
import java.util.Map;

public class GreenMailSupplier implements Supplier<MailServer, InjectMailServer> {

    @Override
    public Class<InjectMailServer> getAnnotationClass() {
        return InjectMailServer.class;
    }

    @Override
    public Class<MailServer> getValueType() {
        return MailServer.class;
    }

    @Override
    public MailServer getValue(InstanceContext<MailServer, InjectMailServer> instanceContext) {
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class);
        RealmRepresentation representation = realm.admin().toRepresentation();

        Map<String, String> config = new HashMap<>();
        config.put("from", "auto@keycloak.org");
        config.put("host", "localhost");
        config.put("port", "3025");

        representation.setSmtpServer(config);
        realm.admin().update(representation);

        MailServer mailServer = new MailServer();
        mailServer.start();
        return mailServer;
    }

    @Override
    public void close(InstanceContext<MailServer, InjectMailServer> instanceContext) {
        instanceContext.getValue().stop();
    }

    @Override
    public boolean compatible(InstanceContext<MailServer, InjectMailServer> a, RequestedInstance<MailServer, InjectMailServer> b) {
        return true;
    }
}
