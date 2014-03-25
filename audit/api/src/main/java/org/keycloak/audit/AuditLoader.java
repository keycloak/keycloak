package org.keycloak.audit;

import org.keycloak.util.ProviderLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuditLoader {

    private AuditLoader() {
    }

    public static AuditListener load(String id) {
        if (id == null) {
            throw new NullPointerException();
        }

        for (AuditListener l : load()) {
            if (id.equals(l.getId())) {
                return l;
            }
        }

        return null;
    }

    public static Iterable<AuditListener> load() {
        return ProviderLoader.load(AuditListener.class);
    }

}
