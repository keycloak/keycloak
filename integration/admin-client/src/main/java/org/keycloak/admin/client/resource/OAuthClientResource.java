package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface OAuthClientResource {

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers();

    // TODO
    // @Path("certificates/{attr}")
    // public ClientAttributeCertificateResource getCertficateResource(@PathParam("attr") String attributePrefix);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OAuthClientRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(OAuthClientRepresentation oAuthClientRepresentation);

    @DELETE
    public void remove();

    @POST
    @Path("client-secret")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation generateNewSecret();

    @GET
    @Path("client-secret")
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation getSecret();

    @GET
    @Path("installation")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstallationJson();

    @Path("/scope-mappings")
    public RoleMappingResource getScopeMappings();

}
