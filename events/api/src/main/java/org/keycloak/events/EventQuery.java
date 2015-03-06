package org.keycloak.events;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EventQuery {

    public EventQuery type(EventType... types);

    public EventQuery realm(String realmId);

    public EventQuery client(String clientId);

    public EventQuery user(String userId);

    public EventQuery fromDate(String fromDate);

    public EventQuery toDate(String toDate);

    public EventQuery ipAddress(String ipAddress);

    public EventQuery firstResult(int result);

    public EventQuery maxResults(int results);

    public List<Event> getResultList();

}
