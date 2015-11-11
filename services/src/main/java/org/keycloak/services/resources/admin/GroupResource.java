package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Bill Burke
 */
public class GroupResource {

    private static Logger logger = Logger.getLogger(GroupResource.class);

    private final RealmModel realm;
    private final KeycloakSession session;
    private final RealmAuth auth;
    private final AdminEventBuilder adminEvent;

    public GroupResource(RealmModel realm, KeycloakSession session, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @Context private UriInfo uriInfo;

    public GroupResource(RealmAuth auth, RealmModel realm, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    /**
     * Get group hierarchy.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupRepresentation> getGroups() {
        this.auth.requireView();
        return ModelToRepresentation.toGroupHierarchy(realm, false);
    }

    /**
     * Set or create child as a top level group.  This will update the group and set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Path("{id}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRealmGroup(@PathParam("id") String parentId, GroupRepresentation rep) {
        GroupModel parentModel = realm.getGroupById(parentId);
        Response.ResponseBuilder builder = Response.status(204);
        if (parentModel == null) {
            throw new NotFoundException("Could not find parent by id");
        }
        GroupModel child = null;
        if (rep.getId() != null) {
            child = realm.getGroupById(rep.getId());
            if (child == null) {
                throw new NotFoundException("Could not find child by id");
            }
        } else {
            child = realm.createGroup(rep.getName());
            updateGroup(rep, child);
            URI uri = uriInfo.getBaseUriBuilder()
                    .path(uriInfo.getMatchedURIs().get(1))
                    .path(child.getId()).build();
            builder.status(201).location(uri);

        }
        child.setParent(parentModel);
        GroupRepresentation childRep = ModelToRepresentation.toRepresentation(child, true);
        return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();
    }




    /**
     * Does not expand hierarchy.  Subgroups will not be set.
     *
     * @param id
     * @return
     */
    @GET
    @Path("{id}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroupById(@PathParam("id") String id) {
        this.auth.requireView();
        GroupModel group = realm.getGroupById(id);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        return ModelToRepresentation.toRepresentation(group, true);
    }

    /**
     * Update group
     *
     * @param rep
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateGroup(@PathParam("id") String id, GroupRepresentation rep) {
        GroupModel model = realm.getGroupById(id);
        if (model == null) {
            throw new NotFoundException("Could not find group by id");
        }

        updateGroup(rep, model);


    }

    @DELETE
    @Path("{id}")
    public void deleteGroup(@PathParam("id") String id) {
        GroupModel model = realm.getGroupById(id);
        if (model == null) {
            throw new NotFoundException("Could not find group by id");
        }
        realm.removeGroup(model);
    }


    /**
     * Set or create child.  This will just set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Path("{id}/children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addGroup(@PathParam("id") String parentId, GroupRepresentation rep) {
        GroupModel parentModel = realm.getGroupById(parentId);
        Response.ResponseBuilder builder = Response.status(204);
        if (parentModel == null) {
            throw new NotFoundException("Could not find parent by id");
        }
        GroupModel child = null;
        if (rep.getId() != null) {
            child = realm.getGroupById(rep.getId());
            if (child == null) {
                throw new NotFoundException("Could not find child by id");
            }
        } else {
            child = realm.createGroup(rep.getName());
            updateGroup(rep, child);
            URI uri = uriInfo.getBaseUriBuilder()
                                           .path(uriInfo.getMatchedURIs().get(1))
                                           .path(child.getId()).build();
            builder.status(201).location(uri);

        }
        realm.moveGroup(child, parentModel);
        GroupRepresentation childRep = ModelToRepresentation.toRepresentation(child, true);
        return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();
    }

    /**
     * create or add a top level realm groupSet or create child.  This will update the group and set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addTopLevelGroup(GroupRepresentation rep) {
        GroupModel child = null;
        Response.ResponseBuilder builder = Response.status(204);
        if (rep.getId() != null) {
            child = realm.getGroupById(rep.getId());
            if (child == null) {
                throw new NotFoundException("Could not find child by id");
            }
        } else {
            child = realm.createGroup(rep.getName());
            updateGroup(rep, child);
            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(child.getId()).build();
            builder.status(201).location(uri);
        }
        realm.moveGroup(child, null);
        return builder.build();
    }

    public void updateGroup(GroupRepresentation rep, GroupModel model) {
        if (rep.getName() != null) model.setName(rep.getName());

        if (rep.getAttributes() != null) {
            Set<String> attrsToRemove = new HashSet<>(model.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());
            for (Map.Entry<String, List<String>> attr : rep.getAttributes().entrySet()) {
                model.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                model.removeAttribute(attr);
            }
        }
    }

    @Path("{id}/role-mappings")
    public RoleMapperResource getRoleMappings(@PathParam("id") String id) {

        GroupModel group = session.realms().getGroupById(id, realm);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        auth.init(RealmAuth.Resource.USER);

        RoleMapperResource resource =  new RoleMapperResource(realm, auth, group, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;

    }



}
