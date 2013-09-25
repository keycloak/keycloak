package org.keycloak.services.models.picketlink;

import org.keycloak.services.models.ApplicationModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.picketlink.mappings.ApplicationData;
import org.keycloak.services.models.picketlink.relationships.ScopeRelationship;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.sample.Grant;
import org.picketlink.idm.model.sample.Role;
import org.picketlink.idm.model.sample.SampleModel;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.RelationshipQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationAdapter implements ApplicationModel {
    protected ApplicationData resource;
    protected RealmAdapter realm;
    protected IdentityManager idm;
    protected PartitionManager partitionManager;
    protected RelationshipManager relationshipManager;

    public ApplicationAdapter(ApplicationData resource, RealmAdapter realm, PartitionManager partitionManager) {
        this.resource = resource;
        this.realm = realm;
        this.partitionManager = partitionManager;
    }

    protected IdentityManager getIdm() {
        if (idm == null) idm = partitionManager.createIdentityManager(resource);
        return idm;
    }

    protected RelationshipManager getRelationshipManager() {
        if (relationshipManager == null) relationshipManager = partitionManager.createRelationshipManager();
        return relationshipManager;
    }

    @Override
    public void updateResource() {
        partitionManager.update(resource);
    }

    @Override
    public UserAdapter getResourceUser() {
        return new UserAdapter(resource.getResourceUser(), realm.getIdm());
    }

    @Override
    public String getId() {
        // for some reason picketlink queries by name when finding partition, don't know what ID is used for now
        return resource.getName();
    }

    @Override
    public String getName() {
        return resource.getResourceName();
    }

    @Override
    public void setName(String name) {
        resource.setResourceName(name);
    }

    @Override
    public boolean isEnabled() {
        return resource.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        resource.setEnabled(enabled);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return resource.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        resource.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        return resource.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        resource.setManagementUrl(url);
    }

    @Override
    public RoleAdapter getRole(String name) {
        Role role = SampleModel.getRole(getIdm(), name);
        if (role == null) return null;
        return new RoleAdapter(role, getIdm());
    }

    @Override
    public RoleModel getRoleById(String id) {
        IdentityQuery<Role> query = getIdm().createIdentityQuery(Role.class);
        query.setParameter(IdentityType.ID, id);
        List<Role> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return new RoleAdapter(roles.get(0), getIdm());
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        SampleModel.grantRole(getRelationshipManager(), ((UserAdapter) user).getUser(), ((RoleAdapter) role).getRole());
    }



    @Override
    public RoleAdapter addRole(String name) {
        Role role = new Role(name);
        getIdm().add(role);
        return new RoleAdapter(role, getIdm());
    }

    @Override
    public List<RoleModel> getRoles() {
        IdentityQuery<Role> query = getIdm().createIdentityQuery(Role.class);
        query.setParameter(Role.PARTITION, resource);
        List<Role> roles = query.getResultList();
        List<RoleModel> roleModels = new ArrayList<RoleModel>();
        for (Role role : roles) {
            roleModels.add(new RoleAdapter(role, idm));
        }
        return roleModels;
    }

    @Override
    public Set<String> getRoleMappingValues(UserModel user) {
        RelationshipQuery<Grant> query = getRelationshipManager().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, ((UserAdapter)user).getUser());
        List<Grant> grants = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (Grant grant : grants) {
            if (grant.getRole().getPartition().getId().equals(resource.getId())) set.add(grant.getRole().getName());
        }
        return set;
    }

    @Override
    public List<RoleModel> getRoleMappings(UserModel user) {
        RelationshipQuery<Grant> query = getRelationshipManager().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, ((UserAdapter)user).getUser());
        List<Grant> grants = query.getResultList();
        List<RoleModel> set = new ArrayList<RoleModel>();
        for (Grant grant : grants) {
            if (grant.getRole().getPartition().getId().equals(resource.getId())) set.add(new RoleAdapter(grant.getRole(), getIdm()));
        }
        return set;
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        RelationshipQuery<Grant> query = getRelationshipManager().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, ((UserAdapter)user).getUser());
        query.setParameter(Grant.ROLE, ((RoleAdapter)role).getRole());
        List<Grant> grants = query.getResultList();
        for (Grant grant : grants) {
            getRelationshipManager().remove(grant);
        }
    }




    @Override
    public void addScope(UserModel agent, String roleName) {
        IdentityManager idm = getIdm();
        Role role = SampleModel.getRole(idm,roleName);
        if (role == null) throw new RuntimeException("role not found");
        addScope(agent, new RoleAdapter(role, idm));

    }

    @Override
    public void addScope(UserModel agent, RoleModel role) {
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(((UserAdapter)agent).getUser());
        scope.setScope(((RoleAdapter)role).getRole());
        getRelationshipManager().add(scope);
    }

    @Override
    public Set<String> getScope(UserModel agent) {
        RelationshipQuery<ScopeRelationship> query = getRelationshipManager().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, ((UserAdapter)agent).getUser());
        List<ScopeRelationship> scope = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (ScopeRelationship rel : scope) {
            if (rel.getScope().getPartition().getId().equals(resource.getId())) set.add(rel.getScope().getName());
        }
        return set;
    }
}
