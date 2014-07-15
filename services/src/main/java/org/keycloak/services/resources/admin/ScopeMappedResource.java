package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ApplicationMappingsRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for managing the scope mappings of a specific client (application or oauth).
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeMappedResource {
    protected RealmModel realm;
    private RealmAuth auth;
    protected ClientModel client;
    protected KeycloakSession session;

    public ScopeMappedResource(RealmModel realm, RealmAuth auth, ClientModel client, KeycloakSession session) {
        this.realm = realm;
        this.auth = auth;
        this.client = client;
        this.session = session;
    }

    /**
     * Get all scope mappings for this client
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @NoCache
    public MappingsRepresentation getScopeMappings() {
        auth.requireView();

        MappingsRepresentation all = new MappingsRepresentation();
        Set<RoleModel> realmMappings = client.getRealmScopeMappings();
        if (realmMappings.size() > 0) {
            List<RoleRepresentation> realmRep = new ArrayList<RoleRepresentation>();
            for (RoleModel roleModel : realmMappings) {
                realmRep.add(ModelToRepresentation.toRepresentation(roleModel));
            }
            all.setRealmMappings(realmRep);
        }

        List<ApplicationModel> applications = realm.getApplications();
        if (applications.size() > 0) {
            Map<String, ApplicationMappingsRepresentation> appMappings = new HashMap<String, ApplicationMappingsRepresentation>();
            for (ApplicationModel app : applications) {
                Set<RoleModel> roleMappings = app.getApplicationScopeMappings(client);
                if (roleMappings.size() > 0) {
                    ApplicationMappingsRepresentation mappings = new ApplicationMappingsRepresentation();
                    mappings.setApplicationId(app.getId());
                    mappings.setApplication(app.getName());
                    List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
                    mappings.setMappings(roles);
                    for (RoleModel role : roleMappings) {
                        roles.add(ModelToRepresentation.toRepresentation(role));
                    }
                    appMappings.put(app.getName(), mappings);
                    all.setApplicationMappings(appMappings);
                }
            }
        }
        return all;
    }

    /**
     * Get list of realm-level roles associated with this client's scope.
     *
     * @return
     */
    @Path("realm")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getRealmScopeMappings() {
        auth.requireView();

        Set<RoleModel> realmMappings = client.getRealmScopeMappings();
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : realmMappings) {
            realmMappingsRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return realmMappingsRep;
    }

    /**
     * Get list of realm-level roles that are available to attach to this client's scope.
     *
     * @return
     */
    @Path("realm/available")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getAvailableRealmScopeMappings() {
        auth.requireView();

        Set<RoleModel> roles = realm.getRoles();
        return getAvailable(roles);
    }

    private List<RoleRepresentation> getAvailable(Set<RoleModel> roles) {
        List<RoleRepresentation> available = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (client.hasScope(roleModel)) continue;
            available.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return available;
    }

    /**
     * Get all effective realm-level roles that are associated with this client's scope.  What this does is recurse
     * any composite roles associated with the client's scope and adds the roles to this lists.  The method is really
     * to show a comprehensive total view of realm-level roles associated with the client.
     *
     * @return
     */
    @Path("realm/composite")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getCompositeRealmScopeMappings() {
        auth.requireView();

        Set<RoleModel> roles = realm.getRoles();
        return getComposite(roles);
    }

    private List<RoleRepresentation> getComposite(Set<RoleModel> roles) {
        List<RoleRepresentation> composite = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (client.hasScope(roleModel)) composite.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return composite;
    }

    /**
     * Add a set of realm-level roles to the client's scope
     *
     * @param roles
     */
    @Path("realm")
    @POST
    @Consumes("application/json")
    public void addRealmScopeMappings(List<RoleRepresentation> roles) {
        auth.requireManage();

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRoleById(role.getId());
            if (roleModel == null) {
                throw new NotFoundException("Role not found");
            }
            client.addScopeMapping(roleModel);
        }


    }

    /**
     * Remove a set of realm-level roles from the client's scope
     *
     * @param roles
     */
    @Path("realm")
    @DELETE
    @Consumes("application/json")
    public void deleteRealmScopeMappings(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (roles == null) {
            Set<RoleModel> roleModels = client.getRealmScopeMappings();
            for (RoleModel roleModel : roleModels) {
                client.deleteScopeMapping(roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException("Role not found");
                }
                client.deleteScopeMapping(roleModel);
            }
        }
    }

    /**
     * Get the roles associated with a client's scope for a specific application.
     *
     * @param appName roles associated with client's scope for a specific application
     * @return
     */
    @Path("applications/{app}")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getApplicationScopeMappings(@PathParam("app") String appName) {
        auth.requireView();

        ApplicationModel app = realm.getApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException("Role not found");
        }

        Set<RoleModel> mappings = app.getApplicationScopeMappings(client);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mapRep;
    }

    /**
     * The available application-level roles that can be associated with the client's scope
     *
     * @param appName available roles for a specific application
     * @return
     */
    @Path("applications/{app}/available")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getAvailableApplicationScopeMappings(@PathParam("app") String appName) {
        auth.requireView();

        ApplicationModel app = realm.getApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException("Role not found");
        }

        Set<RoleModel> roles = app.getRoles();
        return getAvailable(roles);
    }

    /**
     * Get effective application roles that are associated with the client's scope for a specific application.
     *
     * @param appName effective roles for a specific app
     * @return
     */
    @Path("applications/{app}/composite")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getCompositeApplicationScopeMappings(@PathParam("app") String appName) {
        auth.requireView();

        ApplicationModel app = realm.getApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException("Role not found");
        }

        Set<RoleModel> roles = app.getRoles();
        return getComposite(roles);
    }

    /**
     * Add application-level roles to the client's scope
     *
     * @param appName
     * @param roles
     */
    @Path("applications/{app}")
    @POST
    @Consumes("application/json")
    public void addApplicationScopeMapping(@PathParam("app") String appName, List<RoleRepresentation> roles) {
        auth.requireManage();

        ApplicationModel app = realm.getApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException("Application not found");
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = app.getRole(role.getName());
            if (roleModel == null) {
                throw new NotFoundException("Role not found");
            }
            client.addScopeMapping(roleModel);
        }

    }

    /**
     * Remove application-level roles from the client's scope.
     *
     * @param appName
     * @param roles
     */
    @Path("applications/{app}")
    @DELETE
    @Consumes("application/json")
    public void deleteApplicationScopeMapping(@PathParam("app") String appName, List<RoleRepresentation> roles) {
        auth.requireManage();

        ApplicationModel app = realm.getApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException("Application not found");
        }

        if (roles == null) {
            Set<RoleModel> roleModels = app.getApplicationScopeMappings(client);
            for (RoleModel roleModel : roleModels) {
                client.deleteScopeMapping(roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = app.getRole(role.getName());
                if (roleModel == null) {
                    throw new NotFoundException("Role not found");
                }
                client.deleteScopeMapping(roleModel);
            }
        }
    }
}
