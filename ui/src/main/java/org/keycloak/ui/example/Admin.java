package org.keycloak.ui.example;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@ApplicationPath("ui/api")
@Path("")
public class Admin extends javax.ws.rs.core.Application {

    private static Map<String, Application> applications = new HashMap<String, Application>();

    private static Map<String, Realm> realms = new HashMap<String, Realm>();

    private static Map<String, List<User>> users = new HashMap<String, List<User>>();

    private static Map<Id, List<String>> roles = new HashMap<Id, List<String>>();

    static {
        Realm realm = new Realm();
        realm.setId(UUID.randomUUID().toString());
        realm.setName("Test realm");
        realm.setEnabled(true);
        realm.setRoles(new String[] { "admin", "user" });
        realms.put(realm.getId(), realm);

        User user = new User();
        user.setUserId("user");
        user.setPassword("password");

        List<User> l = new LinkedList<User>();
        l.add(user);

        users.put(realm.getId(), l);
    }

    @DELETE
    @Path("applications/{id}")
    public void deleteApplication(@PathParam("id") String id) {
        applications.remove(id);
    }

    @DELETE
    @Path("realms/{id}")
    public void deleteRealm(@PathParam("id") String id) {
        realms.remove(id);
    }

    @GET
    @Path("applications/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Application getApplication(@PathParam("id") String id) {
        return applications.get(id);
    }

    @GET
    @Path("applications")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> getApplications() {
        return new LinkedList<Application>(applications.values());
    }

    @GET
    @Path("realms/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Realm getRealm(@PathParam("id") String id) {
        return realms.get(id);
    }

    @GET
    @Path("realms")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Realm> getRealms() {
        return new LinkedList<Realm>(realms.values());
    }

    @POST
    @Path("applications")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(Application application) {
        String id = UUID.randomUUID().toString();
        application.setId(id);
        applications.put(id, application);
        return Response.created(URI.create("/applications/" + id)).build();
    }

    @POST
    @Path("realms")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response save(Realm realm) {
        String id = UUID.randomUUID().toString();
        realm.setId(id);
        realms.put(id, realm);
        return Response.created(URI.create("/realms/" + id)).build();
    }

    @PUT
    @Path("applications/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void save(@PathParam("id") String id, Application application) {
        try {
            deleteApplication(id);
        } catch (WebApplicationException e) {
        }

        applications.put(id, application);
    }

    @PUT
    @Path("realms/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void save(@PathParam("id") String id, Realm realm) {
        try {
            deleteRealm(id);
        } catch (WebApplicationException e) {
        }

        realms.put(id, realm);
    }

    @GET
    @Path("realms/{realm}/users/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User getUser(@PathParam("realm") String realm, @PathParam("id") String id) {
        for (User u : getUsers(realm)) {
            if (u.getUserId().equals(id)) {
                return u;
            }
        }
        throw new WebApplicationException(Status.NOT_FOUND);
    }

    @GET
    @Path("realms/{realm}/users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getUsers(@PathParam("realm") String realm) {
        List<User> l = users.get(realm);
        if (l == null) {
            l = new LinkedList<User>();
            users.put(realm, l);
        }
        return l;
    }

    @PUT
    @Path("realms/{realm}/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void save(@PathParam("realm") String realm, @PathParam("id") String id, User user) {
        try {
            deleteUser(realm, id);
        } catch (WebApplicationException e) {
        }

        getUsers(realm).add(user);
    }

    @DELETE
    @Path("realms/{realm}/users/{id}")
    public void deleteUser(@PathParam("realm") String realm, @PathParam("id") String id) {
        getUsers(realm).remove(getUser(realm, id));
    }

    @GET
    @Path("roles/{realm}/{role}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getRoleMapping(@PathParam("realm") String realm, @PathParam("role") String role) {
        List<String> ids = getRoleMapping(new Id(realm, role));

        List<User> users = new LinkedList<User>();
        List<User> realmUsers = getUsers(realm);
        for (String id : ids) {
            for (User u : realmUsers) {
                if (u.getUserId().equals(id)) {
                    users.add(u);
                }
            }
        }

        return users;
    }

    private List<String> getRoleMapping(Id id) {
        List<String> l = roles.get(id);
        if (l == null) {
            l = new LinkedList<String>();
            roles.put(id, l);
        }
        return l;
    }

    @PUT
    @Path("roles/{realm}/{role}/{user}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addRoleMapping(@PathParam("realm") String realm, @PathParam("role") String role,
            @PathParam("user") String user, User u) {
        getRoleMapping(new Id(realm, role)).add(user);
    }

    @DELETE
    @Path("roles/{realm}/{role}/{user}")
    public void deleteRoleMapping(@PathParam("realm") String realm, @PathParam("role") String role,
            @PathParam("user") String user) {
        Iterator<String> itr = getRoleMapping(new Id(realm, role)).iterator();
        while (itr.hasNext()) {
            if (itr.next().equals(user)) {
                itr.remove();
                return;
            }
        }
        throw new WebApplicationException(Status.NOT_FOUND);
    }
}