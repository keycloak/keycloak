package org.keycloak.examples.providers.audit;

import org.keycloak.Config;
import org.keycloak.audit.AuditListener;
import org.keycloak.audit.AuditListenerFactory;
import org.keycloak.audit.EventType;
import org.keycloak.provider.ProviderSession;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SysoutAuditListenerFactory implements AuditListenerFactory {

    private Set<EventType> excludedEvents;

    @Override
    public AuditListener create(ProviderSession providerSession) {
        return new SysoutAuditListener(excludedEvents);
    }

    @Override
    public void init(Config.Scope config) {
        String excludes = config.get("excludes");
        if (excludes != null) {
            excludedEvents = new HashSet<EventType>();
            for (String e : excludes.split(",")) {
                excludedEvents.add(EventType.valueOf(e));
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "sysout";
    }

}
