package org.keycloak.performance.dataset;

import java.io.IOException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
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
        logger().debug("Creating " + this);
        boolean conflict = false;
        try {
            Response response = create(adminClient);
            if (response == null) {
                readAndSetId(adminClient);
            } else {
                String responseBody = response.readEntity(String.class);
                response.close();
                switch (response.getStatus()) {
                    case 201: // created
                        if (responseBody != null && !responseBody.isEmpty()) {
                            logger().trace(String.format("Response status: %s, body: %s", response.getStatus(), responseBody));
                            setRepresentation(EntityTemplate.OBJECT_MAPPER.readValue(responseBody, (Class<REP>) getRepresentation().getClass()));
                        } else {
                            setId(getCreatedId(response));
                        }
                        break;
                    case 409: // some client endpoints dont't throw exception on 409 response, throwing from here
                        throw new ClientErrorException(HTTP_409_SUFFIX, response);
                    default:
                        throw new RuntimeException(String.format("Error when creating entity %s.", this), new WebApplicationException(response));
                }
            }
        } catch (ClientErrorException ex) {
            if (ex.getResponse().getStatus() == 409) {
                conflict = true;
                logger().debug(String.format("Entity %s already exists.", this));
                readAndSetId(adminClient);
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
