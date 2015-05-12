package org.keycloak.events;

import org.keycloak.events.admin.AdminEventQuery;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EventStoreProvider extends EventListenerProvider {

    public EventQuery createQuery();

    public AdminEventQuery createAdminQuery();

    public void clear();

    public void clear(String realmId);

    public void clear(String realmId, long olderThan);

    public void clearAdmin();

    public void clearAdmin(String realmId);

    public void clearAdmin(String realmId, long olderThan);

}
