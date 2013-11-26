package org.keycloak.models.picketlink;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.picketlink.mappings.ApplicationData;
import org.keycloak.models.picketlink.relationships.ScopeRelationship;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.sample.Grant;
import org.picketlink.idm.model.sample.Role;
import org.picketlink.idm.model.sample.SampleModel;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.RelationshipQuery;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationAdapter implements ApplicationModel {
    protected ApplicationData applicationData;
    protected RealmAdapter realm;
    protected IdentityManager idm;
    protected PartitionManager partitionManager;
    protected RelationshipManager relationshipManager;

    public ApplicationAdapter(ApplicationData applicationData, RealmAdapter realm, PartitionManager partitionManager) {
        this.applicationData = applicationData;
        this.realm = realm;
        this.partitionManager = partitionManager;
    }

    protected IdentityManager getIdm() {
        if (idm == null) idm = partitionManager.createIdentityManager(applicationData);
        return idm;
    }

    protected RelationshipManager getRelationshipManager() {
        if (relationshipManager == null) relationshipManager = partitionManager.createRelationshipManager();
        return relationshipManager;
    }

    @Override
    public void updateApplication() {
        partitionManager.update(applicationData);
    }

    @Override
    public UserAdapter getApplicationUser() {
        return new UserAdapter(applicationData.getResourceUser(), realm.getIdm());
    }

    @Override
    public String getId() {
        // for some reason picketlink queries by name when finding partition, don't know what ID is used for now
        return applicationData.getName();
    }

    @Override
    public String getName() {
        return applicationData.getResourceName();
    }

    @Override
    public void setName(String name) {
        applicationData.setResourceName(name);
        updateApplication();
    }

    @Override
    public boolean isEnabled() {
        return applicationData.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        applicationData.setEnabled(enabled);
        updateApplication();
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return applicationData.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        applicationData.setSurrogateAuthRequired(surrogateAuthRequired);
        updateApplication();
    }

    @Override
    public String getManagementUrl() {
        return applicationData.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        applicationData.setManagementUrl(url);
        updateApplication();
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
    public boolean hasRole(UserModel user, RoleModel role) {
        return SampleModel.hasRole(getRelationshipManager(), ((UserAdapter) user).getUser(), ((RoleAdapter) role).getRole());
    }

    @Override
    public boolean hasRole(UserModel user, String role) {
        RoleModel roleModel = getRole(role);
        return hasRole(user, roleModel);
    }

    @Override
    public RoleAdapter addRole(String name) {
        Role role = new Role(name);
        getIdm().add(role);
        return new RoleAdapter(role, getIdm());
    }

    @Override
    public boolean removeRole(String id) {
        try {
            getIdm().remove(getIdm().lookupIdentityById(Role.class, id));
            return true;
        } catch (IdentityManagementException e) {
            return false;
        }
    }

    @Override
    public List<RoleModel> getRoles() {
        IdentityQuery<Role> query = getIdm().createIdentityQuery(Role.class);
        query.setParameter(Role.PARTITION, applicationData);
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
            if (grant.getRole().getPartition().getId().equals(applicationData.getId())) set.add(grant.getRole().getName());
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
            if (grant.getRole().getPartition().getId().equals(applicationData.getId())) set.add(new RoleAdapter(grant.getRole(), getIdm()));
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
    public void addScopeMapping(UserModel agent, String roleName) {
        IdentityManager idm = getIdm();
        Role role = SampleModel.getRole(idm,roleName);
        if (role == null) throw new RuntimeException("role not found");
        addScopeMapping(agent, new RoleAdapter(role, idm));

    }

    @Override
    public void addScopeMapping(UserModel agent, RoleModel role) {
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(((UserAdapter)agent).getUser());
        scope.setScope(((RoleAdapter)role).getRole());
        getRelationshipManager().add(scope);
    }

    @Override
    public void deleteScopeMapping(UserModel user, RoleModel role) {
        RelationshipQuery<ScopeRelationship> query = getRelationshipManager().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, ((UserAdapter)user).getUser());
        query.setParameter(ScopeRelationship.SCOPE, ((RoleAdapter)role).getRole());
        List<ScopeRelationship> grants = query.getResultList();
        for (ScopeRelationship grant : grants) {
            getRelationshipManager().remove(grant);
        }
    }


    @Override
    public Set<String> getScopeMappingValues(UserModel agent) {
        RelationshipQuery<ScopeRelationship> query = getRelationshipManager().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, ((UserAdapter)agent).getUser());
        List<ScopeRelationship> scope = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (ScopeRelationship rel : scope) {
            if (rel.getScope().getPartition().getId().equals(applicationData.getId())) set.add(rel.getScope().getName());
        }
        return set;
    }

    @Override
    public List<RoleModel> getScopeMappings(UserModel agent) {
        RelationshipQuery<ScopeRelationship> query = getRelationshipManager().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, ((UserAdapter)agent).getUser());
        List<ScopeRelationship> scope = query.getResultList();
        List<RoleModel> roles = new ArrayList<RoleModel>();
        for (ScopeRelationship rel : scope) {
            if (rel.getScope().getPartition().getId().equals(applicationData.getId())) roles.add(new RoleAdapter(rel.getScope(), getIdm()));
        }
        return roles;
    }

    @Override
    public List<String> getDefaultRoles() {
        if ( applicationData.getDefaultRoles() != null) {
            return Arrays.asList(applicationData.getDefaultRoles());
        }
        else {
            return Collections.emptyList();
        }
    }

    @Override
    public void addDefaultRole(String name) {
        if (getRole(name) == null) {
            addRole(name);
        }

        String[] defaultRoles = applicationData.getDefaultRoles();
        if (defaultRoles == null) {
            defaultRoles = new String[1];
        } else {
            defaultRoles = Arrays.copyOf(defaultRoles, defaultRoles.length + 1);
        }
        defaultRoles[defaultRoles.length - 1] = name;

        applicationData.setDefaultRoles(defaultRoles);
        updateApplication();
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        for (String name : defaultRoles) {
            if (getRole(name) == null) {
                addRole(name);
            }
        }

        applicationData.setDefaultRoles(defaultRoles);
        updateApplication();
    }

}
