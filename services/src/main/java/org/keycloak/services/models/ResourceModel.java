package org.keycloak.services.models;

import org.keycloak.services.models.relationships.ResourceRelationship;
import org.keycloak.services.models.relationships.ScopeRelationship;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Grant;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.Tier;
import org.picketlink.idm.model.User;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.RelationshipQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceModel {
    protected Tier tier;
    protected ResourceRelationship agent;
    protected RealmModel realm;
    protected IdentitySession identitySession;
    protected IdentityManager idm;

    public ResourceModel(Tier tier, ResourceRelationship agent, RealmModel realm, IdentitySession session) {
        this.tier = tier;
        this.agent = agent;
        this.realm = realm;
        this.identitySession = session;
    }

    public IdentityManager getIdm() {
        if (idm == null) idm = identitySession.createIdentityManager(tier);
        return idm;
    }

    public void updateResource() {
        getIdm().update(agent);
    }

    public User getResourceUser() {
        return agent.getResourceUser();
    }

    public String getId() {
        return tier.getId();
    }

    public String getName() {
        return agent.getResourceName();
    }

    public void setName(String name) {
        agent.setResourceName(name);
    }

    public boolean isEnabled() {
        return agent.getEnabled();
    }

    public void setEnabled(boolean enabled) {
        agent.setEnabled(enabled);
    }

    public boolean isSurrogateAuthRequired() {
        return agent.getSurrogateAuthRequired();
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        agent.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    public String getManagementUrl() {
        return agent.getManagementUrl();
    }

    public void setManagementUrl(String url) {
        agent.setManagementUrl(url);
    }

    public List<Role> getRoles() {
        IdentityQuery<Role> query = getIdm().createIdentityQuery(Role.class);
        query.setParameter(Role.PARTITION, tier);
        return query.getResultList();
    }

    public Set<String> getRoleMappings(User user) {
        RelationshipQuery<Grant> query = getIdm().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, user);
        List<Grant> grants = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (Grant grant : grants) {
            if (grant.getRole().getPartition().getId().equals(tier.getId())) set.add(grant.getRole().getName());
        }
        return set;
    }

    public void addScope(Agent agent, String roleName) {
        IdentityManager idm = getIdm();
        Role role = idm.getRole(roleName);
        if (role == null) throw new RuntimeException("role not found");
        addScope(agent, role);

    }

    public void addScope(Agent agent, Role role) {
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(agent);
        scope.setScope(role);
    }

    public Set<String> getScope(Agent agent) {
        RelationshipQuery<ScopeRelationship> query = getIdm().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, agent);
        List<ScopeRelationship> scope = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (ScopeRelationship rel : scope) {
            if (rel.getScope().getPartition().getId().equals(tier.getId())) set.add(rel.getScope().getName());
        }
        return set;
    }
}
