package org.keycloak.audit.jpa;

import org.keycloak.Config;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;
import org.keycloak.audit.EventType;
import org.keycloak.provider.ProviderSession;
import org.keycloak.util.JpaUtils;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaAuditProviderFactory implements AuditProviderFactory {

    public static final String ID = "jpa";
    private EntityManagerFactory emf;

    private Set<EventType> includedEvents = new HashSet<EventType>();

    @Override
    public AuditProvider create(ProviderSession providerSession) {
        return new JpaAuditProvider(emf.createEntityManager(), includedEvents);
    }

    @Override
    public void init(Config.Scope config) {
        emf = Persistence.createEntityManagerFactory("jpa-keycloak-audit-store", JpaUtils.getHibernateProperties());

        String[] include = config.getArray("include-events");
        if (include != null) {
            for (String i : include) {
                includedEvents.add(EventType.valueOf(i.toUpperCase()));
            }
        } else {
            for (EventType i : EventType.values()) {
                includedEvents.add(i);
            }
        }

        String[] exclude = config.getArray("exclude-events");
        if (exclude != null) {
            for (String e : exclude) {
                includedEvents.remove(EventType.valueOf(e.toUpperCase()));
            }
        }
    }

    @Override
    public void close() {
        emf.close();
    }

    @Override
    public String getId() {
        return ID;
    }

}
