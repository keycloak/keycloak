package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.*;
import org.keycloak.representations.idm.ApplicationMappingsRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeMappedResource {
    protected RealmModel realm;
    protected UserModel agent;
    protected KeycloakSession session;

    public ScopeMappedResource(RealmModel realm, UserModel account, KeycloakSession session) {
        this.realm = realm;
        this.agent = account;
        this.session = session;
    }

    @GET
    @Produces("application/json")
    @NoCache
    public MappingsRepresentation getScopeMappings() {
        MappingsRepresentation all = new MappingsRepresentation();
        List<RoleModel> realmMappings = realm.getScopeMappings(agent);
        RealmManager manager = new RealmManager(session);
        if (realmMappings.size() > 0) {
            List<RoleRepresentation> realmRep = new ArrayList<RoleRepresentation>();
            for (RoleModel roleModel : realmMappings) {
                realmRep.add(manager.toRepresentation(roleModel));
            }
            all.setRealmMappings(realmRep);
        }

        List<ApplicationModel> applications = realm.getApplications();
        if (applications.size() > 0) {
            Map<String, ApplicationMappingsRepresentation> appMappings = new HashMap<String, ApplicationMappingsRepresentation>();
            for (ApplicationModel app : applications) {
                List<RoleModel> roleMappings = app.getScopeMappings(agent);
                if (roleMappings.size() > 0) {
                    ApplicationMappingsRepresentation mappings = new ApplicationMappingsRepresentation();
                    mappings.setApplicationId(app.getId());
                    mappings.setApplication(app.getName());
                    List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
                    mappings.setMappings(roles);
                    for (RoleModel role : roleMappings) {
                        roles.add(manager.toRepresentation(role));
                    }
                    appMappings.put(app.getName(), mappings);
                    all.setApplicationMappings(appMappings);
                }
            }
        }
        return all;
    }

    @Path("realm")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getRealmScopeMappings() {
        List<RoleModel> realmMappings = realm.getScopeMappings(agent);
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        RealmManager manager = new RealmManager(session);
        for (RoleModel roleModel : realmMappings) {
            realmMappingsRep.add(manager.toRepresentation(roleModel));
        }
        return realmMappingsRep;
    }

    @Path("realm")
    @POST
    @Consumes("application/json")
    public void addRealmScopeMappings(List<RoleRepresentation> roles) {
        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRoleById(role.getId());
            if (roleModel == null) {
                throw new NotFoundException();
            }
            realm.addScopeMapping(agent, roleModel);
        }


    }

    @Path("realm")
    @DELETE
    @Consumes("application/json")
    public void deleteRealmScopeMappings(List<RoleRepresentation> roles) {
        if (roles == null) {
            List<RoleModel> roleModels = realm.getScopeMappings(agent);
            for (RoleModel roleModel : roleModels) {
                realm.deleteScopeMapping(agent, roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException();
                }
                realm.deleteScopeMapping(agent, roleModel);
            }
        }
    }

    @Path("applications/{appId}")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getApplicationScopeMappings(@PathParam("appId") String appId) {
        ApplicationModel app = realm.getApplicationById(appId);

        if (app == null) {
            throw new NotFoundException();
        }

        List<RoleModel> mappings = app.getScopeMappings(agent);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(RealmManager.toRepresentation(roleModel));
        }
        return mapRep;
    }

    @Path("applications/{appId}")
    @POST
    @Consumes("application/json")
    public void addApplicationScopeMapping(@PathParam("appId") String appId, List<RoleRepresentation> roles) {
        ApplicationModel app = realm.getApplicationById(appId);

        if (app == null) {
            throw new NotFoundException();
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = app.getRoleById(role.getId());
            if (roleModel == null) {
                throw new NotFoundException();
            }
            app.addScopeMapping(agent, roleModel);
        }

    }

    @Path("applications/{appId}")
    @DELETE
    @Consumes("application/json")
    public void deleteApplicationRoleMapping(@PathParam("appId") String appId, List<RoleRepresentation> roles) {
        ApplicationModel app = realm.getApplicationById(appId);

        if (app == null) {
            throw new NotFoundException();
        }

        if (roles == null) {
            List<RoleModel> roleModels = app.getScopeMappings(agent);
            for (RoleModel roleModel : roleModels) {
                app.deleteScopeMapping(agent, roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = app.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException();
                }
                app.deleteScopeMapping(agent, roleModel);
            }
        }
    }}
