package org.keycloak.connections.jpa.updater;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

import java.sql.Connection;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface JpaUpdaterProvider extends Provider {

    public String FIRST_VERSION = "1.0.0.Final";

    public String LAST_VERSION = "1.4.0";

    public String getCurrentVersionSql(String defaultSchema);

    public void update(KeycloakSession session, Connection connection, String defaultSchema);

    public void validate(Connection connection, String defaultSchema);

}
