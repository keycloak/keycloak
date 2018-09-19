package org.keycloak.performance.dataset;

import java.io.IOException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import static org.keycloak.admin.client.CreatedResponseUtil.getCreatedId;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.performance.templates.EntityTemplate;

/**
 *
 * @author tkyjovsk
 */
public interface Creatable<REP> extends Updatable<REP> {

    public static final String HTTP_409_SUFFIX = "409 Conflict";

    public REP read(Keycloak adminClient);

    public default String getIdAndReadIfNull(Keycloak adminClient) {
        if (getId() == null) {
            logger().debug("id of entity " + this + " was null, reading from server");
            readAndSetId(adminClient);
        }
        return getId();
    }

    public default void readAndSetId(Keycloak adminClient) {
        setId(getIdFromRepresentation(read(adminClient)));
    }

    public Response create(Keycloak adminClient);

    public default boolean createCheckingForConflict(Keycloak adminClient) {
        logger().trace("creating " + this);
        boolean conflict = false;
        try {
            Response response = create(adminClient);
            if (response == null) {
                readAndSetId(adminClient);
            } else {
                String responseBody = response.readEntity(String.class);
                response.close();
                if (response.getStatus() == 409) { // some endpoints dont't throw exception on 409, throwing here
                    throw new ClientErrorException(HTTP_409_SUFFIX, response);
                }
                if (responseBody != null && !responseBody.isEmpty()) {
                    logger().trace(responseBody);
                    setRepresentation(EntityTemplate.OBJECT_MAPPER.readValue(responseBody, (Class<REP>) getRepresentation().getClass()));
                } else {
                    setId(getCreatedId(response));
                }
            }
        } catch (ClientErrorException ex) {
            if (ex.getResponse().getStatus() == 409) {
                conflict = true;
                logger().trace("entity already exists");
                readAndSetId(adminClient);
            } else {
                throw ex;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return conflict;
    }

    public default void createOrUpdateExisting(Keycloak adminClient) {
        if (createCheckingForConflict(adminClient)) {
            update(adminClient);
        }
    }

}
