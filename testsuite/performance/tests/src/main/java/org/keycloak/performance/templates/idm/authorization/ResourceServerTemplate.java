package org.keycloak.performance.templates.idm.authorization;

import org.apache.commons.lang.Validate;
import org.keycloak.performance.templates.idm.*;
import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.iteration.ListOfLists;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.templates.NestedEntityTemplateWrapperList;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ResourceServerTemplate extends NestedEntityTemplate<Client, ResourceServer, ResourceServerRepresentation> {

    public final ScopeTemplate scopeTemplate;
    public final ResourceTemplate resourceTemplate;
    public final RolePolicyTemplate rolePolicyTemplate;
    public final JsPolicyTemplate jsPolicyTemplate;
    public final UserPolicyTemplate userPolicyTemplate;
    public final ClientPolicyTemplate clientPolicyTemplate;
    public final ResourcePermissionTemplate resourcePermissionTemplate;
    public final ScopePermissionTemplate scopePermissionTemplate;

    public final int maxPolicies;

    public ResourceServerTemplate(ClientTemplate clientTemplate) {
        super(clientTemplate);
        this.scopeTemplate = new ScopeTemplate(this);
        this.resourceTemplate = new ResourceTemplate(this);
        this.rolePolicyTemplate = new RolePolicyTemplate(this);
        this.jsPolicyTemplate = new JsPolicyTemplate(this);
        this.userPolicyTemplate = new UserPolicyTemplate(this);
        this.clientPolicyTemplate = new ClientPolicyTemplate(this);
        this.resourcePermissionTemplate = new ResourcePermissionTemplate(this);
        this.scopePermissionTemplate = new ScopePermissionTemplate(this);

        this.maxPolicies = rolePolicyTemplate.rolePoliciesPerResourceServer
                + jsPolicyTemplate.jsPoliciesPerResourceServer
                + userPolicyTemplate.userPoliciesPerResourceServer
                + clientPolicyTemplate.clientPoliciesPerResourceServer;
    }

    public ClientTemplate clientTemplate() {
        return (ClientTemplate) getParentEntityTemplate();
    }

    @Override
    public void validateConfiguration() {
        scopeTemplate.validateConfiguration();
        resourceTemplate.validateConfiguration();
        rolePolicyTemplate.validateConfiguration();
        jsPolicyTemplate.validateConfiguration();
        userPolicyTemplate.validateConfiguration();
        clientPolicyTemplate.validateConfiguration();
        resourcePermissionTemplate.validateConfiguration();
        scopePermissionTemplate.validateConfiguration();
    }

    @Override
    public ResourceServer newEntity(Client client) {
        Validate.notNull(client);
        Validate.notNull(client.getRepresentation());
        Validate.notNull(client.getRepresentation().getBaseUrl());
        return new ResourceServer(client);
    }

    @Override
    public void processMappings(ResourceServer resourceServer) {
        resourceServer.setScopes(new NestedEntityTemplateWrapperList<>(resourceServer, scopeTemplate));
        resourceServer.setResources(new NestedEntityTemplateWrapperList<>(resourceServer, resourceTemplate));

        resourceServer.setRolePolicies(new NestedEntityTemplateWrapperList<>(resourceServer, rolePolicyTemplate));
        resourceServer.setJsPolicies(new NestedEntityTemplateWrapperList<>(resourceServer, jsPolicyTemplate));
        resourceServer.setUserPolicies(new NestedEntityTemplateWrapperList<>(resourceServer, userPolicyTemplate));
        resourceServer.setClientPolicies(new NestedEntityTemplateWrapperList<>(resourceServer, clientPolicyTemplate));
        resourceServer.setAllPolicies(new ListOfLists( // proxy list
                resourceServer.getRolePolicies(),
                resourceServer.getJsPolicies(),
                resourceServer.getUserPolicies(),
                resourceServer.getClientPolicies()
        ));

        resourceServer.setResourcePermissions(new NestedEntityTemplateWrapperList<>(resourceServer, resourcePermissionTemplate));
        resourceServer.setScopePermissions(new NestedEntityTemplateWrapperList<>(resourceServer, scopePermissionTemplate));

    }

    @Override
    public int getEntityCountPerParent() { // parent is Client
        return 1;
    }

    @Override
    public ResourceServer newEntity(Client parentEntity, int index) {
        return newEntity(parentEntity);
    }

}
