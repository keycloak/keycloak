package org.keycloak.performance.dataset;

import javax.ws.rs.NotFoundException;
import org.keycloak.admin.client.Keycloak;

/**
 * For entities with no id.
 *
 * @author tkyjovsk
 */
public interface Updatable<REP> extends Representable<REP> {

    public void update(Keycloak adminClient);

    public void delete(Keycloak adminClient);

    public default void deleteOrIgnoreMissing(Keycloak adminClient) {
        try {
            delete(adminClient);
        } catch (NotFoundException ex) {
            logger().info(String.format("Entity %s not found. Considering as deleted.", this));
        }
    }

}
