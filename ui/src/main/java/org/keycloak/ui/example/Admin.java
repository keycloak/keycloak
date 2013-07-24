package org.keycloak.ui.example;


import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationPath("ui/api")
@Path("")
public class Admin extends javax.ws.rs.core.Application {

    private static Map<String, Realm> realms = new HashMap<String, Realm>();

    private static Map<String, Application> applications = new HashMap<String, Application>();

    @DELETE
    @Path("/applications/{key}")
    public void delete(@PathParam("key") String applicationKey) {
        applications.remove(applicationKey);
    }

    @DELETE
    @Path("/realms/{key}")
    public void deleteRealm(@PathParam("key") String key) {
        realms.remove(key);
    }

    @GET
    @Path("/applications/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Application getApplication(@PathParam("key") String applicationKey) {
        return applications.get(applicationKey);
    }

    @GET
    @Path("/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> getApplications() {
        return new LinkedList<Application>(applications.values());
    }

    @GET
    @Path("/realms/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Realm getRealm(@PathParam("key") String key) {
        return realms.get(key);
    }


    @GET
    @Path("/realms")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Realm> getRealms() {
        return new LinkedList<Realm>(realms.values());
    }

    @POST
    @Path("/applications")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(Application application) {
        String key = UUID.randomUUID().toString();
        application.setKey(key);
        applications.put(key, application);
        return Response.created(URI.create("/applications/" + application.getKey())).build();
    }

    @POST
    @Path("/realms")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(Realm realm) {
        String key = UUID.randomUUID().toString();
        realm.setKey(key);
        realms.put(key, realm);
        return Response.created(URI.create("/realms/" + realm.getKey())).build();
    }

    @PUT
    @Path("/applications/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void save(@PathParam("key") String applicationKey, Application application) {
        applications.put(applicationKey, application);
    }

    @PUT
    @Path("/realms/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void save(@PathParam("key") String key, Realm realm) {
        realms.put(key, realm);
    }
}