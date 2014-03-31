package org.keycloak.audit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AuditListener {

    public String getId();

    public void onEvent(Event event);

}
