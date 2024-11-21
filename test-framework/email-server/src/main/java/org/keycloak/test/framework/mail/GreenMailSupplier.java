package org.keycloak.test.framework.mail;

import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierOrder;
import org.keycloak.test.framework.mail.annotations.InjectMailServer;
import org.keycloak.test.framework.realm.RealmConfigBuilder;
import org.keycloak.test.framework.realm.RealmConfigInterceptor;

public class GreenMailSupplier implements Supplier<MailServer, InjectMailServer>, RealmConfigInterceptor<MailServer, InjectMailServer> {

    private final String HOSTNAME = "localhost";
    private final int PORT = 3025;
    private final String FROM = "auto@keycloak.org";

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
        return new MailServer(HOSTNAME, PORT);
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
    public RealmConfigBuilder intercept(RealmConfigBuilder realm, InstanceContext<MailServer, InjectMailServer> instanceContext) {
        return realm.smtp(HOSTNAME, PORT, FROM);
    }

    @Override
    public int order() {
        return SupplierOrder.BEFORE_REALM;
    }
}
