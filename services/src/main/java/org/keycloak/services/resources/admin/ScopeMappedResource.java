package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ApplicationMappingsRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeMappedResource {
    protected RealmModel realm;
    private RealmAuth auth;
    protected UserModel agent;
    protected KeycloakSession session;

    public ScopeMappedResource(RealmModel realm, RealmAuth auth, UserModel account, KeycloakSession session) {
        this.realm = realm;
        this.auth = auth;
        this.agent = account;
        this.session = session;
    }

    @GET
    @Produces("application/json")
    @NoCache
    public MappingsRepresentation getScopeMappings() {
        auth.requireView();

        MappingsRepresentation all = new MappingsRepresentation();
        Set<RoleModel> realmMappings = realm.getRealmScopeMappings(agent);
        RealmManager manager = new RealmManager(session);
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
                Set<RoleModel> roleMappings = app.getApplicationScopeMappings(agent);
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

    @Path("realm")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getRealmScopeMappings() {
        auth.requireView();

        Set<RoleModel> realmMappings = realm.getRealmScopeMappings(agent);
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        RealmManager manager = new RealmManager(session);
        for (RoleModel roleModel : realmMappings) {
            realmMappingsRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return realmMappingsRep;
    }

    @Path("realm")
    @POST
    @Consumes("application/json")
    public void addRealmScopeMappings(List<RoleRepresentation> roles) {
        auth.requireManage();

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
        auth.requireManage();

        if (roles == null) {
            Set<RoleModel> roleModels = realm.getRealmScopeMappings(agent);
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

    @Path("applications/{app}")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getApplicationScopeMappings(@PathParam("app") String appName) {
        auth.requireView();

        ApplicationModel app = realm.getApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException();
        }

        Set<RoleModel> mappings = app.getApplicationScopeMappings(agent);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mapRep;
    }

    @Path("applications/{app}")
    @POST
    @Consumes("application/json")
    public void addApplicationScopeMapping(@PathParam("app") String appName, List<RoleRepresentation> roles) {
        auth.requireManage();

        ApplicationModel app = realm.getApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException();
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = app.getRoleById(role.getId());
            if (roleModel == null) {
                throw new NotFoundException();
            }
            realm.addScopeMapping(agent, roleModel);
        }

    }

    @Path("applications/{app}")
    @DELETE
    @Consumes("application/json")
    public void deleteApplicationScopeMapping(@PathParam("app") String appName, List<RoleRepresentation> roles) {
        auth.requireManage();

        ApplicationModel app = realm.getApplicationByName(appName);

        if (app == null) {
            throw new NotFoundException();
        }

        if (roles == null) {
            Set<RoleModel> roleModels = app.getApplicationScopeMappings(agent);
            for (RoleModel roleModel : roleModels) {
                realm.deleteScopeMapping(agent, roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = app.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException();
                }
                realm.deleteScopeMapping(agent, roleModel);
            }
        }
    }
}
