package org.keycloak.services.models.picketlink;

import org.keycloak.services.models.ResourceModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.picketlink.relationships.ResourceRelationship;
import org.keycloak.services.models.picketlink.relationships.ScopeRelationship;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.Grant;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.SimpleRole;
import org.picketlink.idm.model.Tier;
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
public class ResourceAdapter implements ResourceModel {
    protected Tier tier;
    protected ResourceRelationship agent;
    protected RealmAdapter realm;
    protected IdentitySession identitySession;
    protected IdentityManager idm;

    public ResourceAdapter(Tier tier, ResourceRelationship agent, RealmAdapter realm, IdentitySession session) {
        this.tier = tier;
        this.agent = agent;
        this.realm = realm;
        this.identitySession = session;
    }

    protected IdentityManager getIdm() {
        if (idm == null) idm = identitySession.createIdentityManager(tier);
        return idm;
    }

    @Override
    public void updateResource() {
        getIdm().update(agent);
    }

    @Override
    public UserAdapter getResourceUser() {
        return new UserAdapter(agent.getResourceUser(), realm.getIdm());
    }

    @Override
    public String getId() {
        return tier.getId();
    }

    @Override
    public String getName() {
        return agent.getResourceName();
    }

    @Override
    public void setName(String name) {
        agent.setResourceName(name);
    }

    @Override
    public boolean isEnabled() {
        return agent.getEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        agent.setEnabled(enabled);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return agent.getSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        agent.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        return agent.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        agent.setManagementUrl(url);
    }

    @Override
    public RoleAdapter getRole(String name) {
        Role role = getIdm().getRole(name);
        if (role == null) return null;
        return new RoleAdapter(role, getIdm());
    }

    @Override
    public RoleAdapter addRole(String name) {
        Role role = new SimpleRole(name);
        getIdm().add(role);
        return new RoleAdapter(role, getIdm());
    }

    @Override
    public List<RoleModel> getRoles() {
        IdentityQuery<Role> query = getIdm().createIdentityQuery(Role.class);
        query.setParameter(Role.PARTITION, tier);
        List<Role> roles = query.getResultList();
        List<RoleModel> roleModels = new ArrayList<RoleModel>();
        for (Role role : roles) {
            roleModels.add(new RoleAdapter(role, idm));
        }
        return roleModels;
    }

    @Override
    public Set<String> getRoleMappings(UserModel user) {
        RelationshipQuery<Grant> query = getIdm().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, ((UserAdapter)user).getUser());
        List<Grant> grants = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (Grant grant : grants) {
            if (grant.getRole().getPartition().getId().equals(tier.getId())) set.add(grant.getRole().getName());
        }
        return set;
    }

    @Override
    public void addScope(UserModel agent, String roleName) {
        IdentityManager idm = getIdm();
        Role role = idm.getRole(roleName);
        if (role == null) throw new RuntimeException("role not found");
        addScope(agent, new RoleAdapter(role, idm));

    }

    @Override
    public void addScope(UserModel agent, RoleModel role) {
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(((UserAdapter)agent).getUser());
        scope.setScope(((RoleAdapter)role).getRole());
    }

    @Override
    public Set<String> getScope(UserModel agent) {
        RelationshipQuery<ScopeRelationship> query = getIdm().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, ((UserAdapter)agent).getUser());
        List<ScopeRelationship> scope = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (ScopeRelationship rel : scope) {
            if (rel.getScope().getPartition().getId().equals(tier.getId())) set.add(rel.getScope().getName());
        }
        return set;
    }
}
