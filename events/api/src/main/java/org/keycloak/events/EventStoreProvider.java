package org.keycloak.events;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EventStoreProvider extends EventListenerProvider {

    public EventQuery createQuery();

    public void clear();

    public void clear(String realmId);

    public void clear(String realmId, long olderThan);

}
