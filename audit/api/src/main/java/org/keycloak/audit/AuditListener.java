package org.keycloak.audit;

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AuditListener extends Provider {

    public void onEvent(Event event);

}
