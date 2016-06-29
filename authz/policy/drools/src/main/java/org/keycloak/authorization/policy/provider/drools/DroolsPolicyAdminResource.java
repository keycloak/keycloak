package org.keycloak.authorization.policy.provider.drools;

import org.keycloak.authorization.admin.representation.PolicyRepresentation;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.policy.provider.PolicyProviderAdminService;
import org.kie.api.runtime.KieContainer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DroolsPolicyAdminResource implements PolicyProviderAdminService {

    private final ResourceServer resourceServer;
    private final DroolsPolicyProviderFactory factory;

    public DroolsPolicyAdminResource(ResourceServer resourceServer, DroolsPolicyProviderFactory factory) {
        this.resourceServer = resourceServer;
        this.factory = factory;
    }

    @Override
    public void onCreate(Policy policy) {
        this.factory.update(policy);
    }

    @Override
    public void onUpdate(Policy policy) {
        this.factory.update(policy);
    }

    @Override
    public void onRemove(Policy policy) {
        this.factory.remove(policy);
    }

    @Path("/resolveModules")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response resolveModules(PolicyRepresentation policy) {
        return Response.ok(getContainer(policy).getKieBaseNames()).build();
    }

    @Path("/resolveSessions")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response resolveSessions(PolicyRepresentation policy) {
        return Response.ok(getContainer(policy).getKieSessionNamesInKieBase(policy.getConfig().get("moduleName"))).build();
    }

    private KieContainer getContainer(PolicyRepresentation policy) {
        String groupId = policy.getConfig().get("mavenArtifactGroupId");
        String artifactId = policy.getConfig().get("mavenArtifactId");
        String version = policy.getConfig().get("mavenArtifactVersion");
        return this.factory.getKieContainer(groupId, artifactId, version);
    }
}
