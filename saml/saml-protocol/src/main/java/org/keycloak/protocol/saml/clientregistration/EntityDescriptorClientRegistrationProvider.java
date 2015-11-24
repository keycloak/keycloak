package org.keycloak.protocol.saml.clientregistration;

import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.EntityDescriptorDescriptionConverter;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientregistration.AbstractClientRegistrationProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EntityDescriptorClientRegistrationProvider extends AbstractClientRegistrationProvider {

    public EntityDescriptorClientRegistrationProvider(KeycloakSession session) {
        super(session);
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSaml(String descriptor) {
        ClientRepresentation client = session.getProvider(ClientDescriptionConverter.class, EntityDescriptorDescriptionConverter.ID).convertToInternal(descriptor);
        client = create(client);
        URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
        return Response.created(uri).entity(client).build();
    }

}
