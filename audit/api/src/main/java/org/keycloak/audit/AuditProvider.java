package org.keycloak.audit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AuditProvider extends AuditListener {

    public EventQuery createQuery();

    public void clear();

    public void clear(long olderThan);

}
