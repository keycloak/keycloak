package org.keycloak.audit.jpa;

import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.AuditProviderFactory;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaAuditProviderFactory implements AuditProviderFactory {

    public static final String ID = "jpa";
    private EntityManagerFactory emf;

    @Override
    public AuditProvider create() {
        return new JpaAuditProvider(emf.createEntityManager());
    }

    @Override
    public void init() {
        emf = Persistence.createEntityManagerFactory("jpa-keycloak-audit-store");
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
