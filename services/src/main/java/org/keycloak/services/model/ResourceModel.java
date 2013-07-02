package org.keycloak.services.model;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.internal.IdentityManagerFactory;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Attribute;
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
public class ResourceModel
{
   public static final String RESOURCE_AGENT_ID = "_resource_";
   public static final String RESOURCE_NAME = "name";
   public static final String RESOURCE_SURROGATE_AUTH = "surrogate_auth";

   protected Tier tier;
   protected Agent agent;
   protected RealmModel realm;
   protected IdentityManagerFactory factory;

   public ResourceModel(Tier tier, Agent agent, RealmModel realm, IdentityManagerFactory factory)
   {
      this.tier = tier;
      this.agent = agent;
      this.realm = realm;
      this.factory = factory;
   }

   public IdentityManager getIdm()
   {
      return factory.createIdentityManager(tier);
   }

   public void updateResource()
   {
      getIdm().update(agent);
   }

   public String getId()
   {
      return tier.getId();
   }

   public String getName()
   {
      return (String)agent.getAttribute(RESOURCE_NAME).getValue();
   }

   public void setName(String name)
   {
      agent.setAttribute(new Attribute<String>(RESOURCE_NAME, name));
      getIdm().update(agent);
   }

   public boolean isEnabled()
   {
      return agent.isEnabled();
   }

   public void setEnabled(boolean enabled)
   {
      agent.setEnabled(enabled);
   }

   public boolean isSurrogateAuthRequired()
   {
      return (Boolean)agent.getAttribute(RESOURCE_SURROGATE_AUTH).getValue();
   }

   public void setSurrogateAuthRequired(boolean surrogateAuthRequired)
   {
      agent.setAttribute(new Attribute<Boolean>(RESOURCE_SURROGATE_AUTH, surrogateAuthRequired));
   }

   public List<Role> getRoles()
   {
      IdentityQuery<Role> query = getIdm().createIdentityQuery(Role.class);
      query.setParameter(Role.PARTITION, tier);
      return query.getResultList();
   }

   public Set<String> getRoleMappings(User user)
   {
      RelationshipQuery<Grant> query = getIdm().createRelationshipQuery(Grant.class);
      query.setParameter(Grant.ASSIGNEE, user);
      List<Grant> grants = query.getResultList();
      HashSet<String> set = new HashSet<String>();
      for (Grant grant : grants)
      {
         if (grant.getRole().getPartition().getId().equals(tier.getId()))set.add(grant.getRole().getName());
      }
      return set;
   }

   public void addScope(Agent agent, String roleName)
   {
      IdentityManager idm = getIdm();
      Role role = idm.getRole(roleName);
      if (role == null) throw new RuntimeException("role not found");
      ScopeRelationship scope = new ScopeRelationship();
      scope.setClient(agent);
      scope.setScope(role);

   }


   public Set<String> getScope(Agent agent)
   {
      RelationshipQuery<ScopeRelationship> query = getIdm().createRelationshipQuery(ScopeRelationship.class);
      query.setParameter(ScopeRelationship.CLIENT, agent);
      List<ScopeRelationship> scope = query.getResultList();
      HashSet<String> set = new HashSet<String>();
      for (ScopeRelationship rel : scope)
      {
         if (rel.getScope().getPartition().getId().equals(tier.getId())) set.add(rel.getScope().getName());
      }
      return set;
   }



}
