package org.keycloak.connections.jpa.updater;

import org.keycloak.provider.Provider;

import java.sql.Connection;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface JpaUpdaterProvider extends Provider {

    public String FIRST_VERSION = "1.0.0.Final";

    public String LAST_VERSION = "1.1.0.Beta1";

    public String getCurrentVersionSql();

    public void update(Connection connection);

    public void validate(Connection connection);

}
