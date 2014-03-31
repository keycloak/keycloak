package org.keycloak.audit;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EventQuery {

    public EventQuery event(String event);

    public EventQuery realm(String realmId);

    public EventQuery client(String clientId);

    public EventQuery user(String userId);

    public EventQuery firstResult(int result);

    public EventQuery maxResults(int results);

    public List<Event> getResultList();

}
