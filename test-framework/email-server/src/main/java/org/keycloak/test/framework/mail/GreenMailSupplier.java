package org.keycloak.test.framework.mail;

import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.mail.annotations.InjectMailServer;
import org.keycloak.test.framework.realm.RealmConfigBuilder;

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

    @Override
    public void decorate(Object object, InstanceContext<MailServer, InjectMailServer> instanceContext) {
        if (object instanceof RealmConfigBuilder realm) {
            realm.smtp("localhost", 3025, "auto@keycloak.org");
        }
    }
}
