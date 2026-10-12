package org.keycloak.testframework.realm;

import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.util.ApiUtil;

public class ManagedOrganizationCleanup {

    private final List<OrganizationCleanup> cleanupTasks = new LinkedList<>();

    /**
     * Add a cleanup to be done for the organization after the test is completed
     *
     * @param organizationCleanup the required cleanup
     * @return this cleanup
     */
    public ManagedOrganizationCleanup add(OrganizationCleanup organizationCleanup) {
        cleanupTasks.add(organizationCleanup);
        return this;
    }

    void resetToOriginalRepresentation(OrganizationRepresentation rep) {
        if (cleanupTasks.stream().noneMatch(c -> c instanceof ResetOrganization)) {
            OrganizationRepresentation clone = RepresentationUtils.clone(rep);
            cleanupTasks.add(new ResetOrganization(clone));
        }
    }

    void runCleanupTasks(OrganizationResource organization) {
        List<RuntimeException> failures = new LinkedList<>();
        int total = cleanupTasks.size();
        try {
            for (OrganizationCleanup task : cleanupTasks) {
                try {
                    task.cleanup(organization);
                } catch (RuntimeException e) {
                    failures.add(e);
                }
            }
        } finally {
            cleanupTasks.clear();
        }

        if (!failures.isEmpty()) {
            IllegalStateException failure = new IllegalStateException(
                    "Failed to run %d of %d cleanup tasks for the organization".formatted(failures.size(), total));
            failures.forEach(failure::addSuppressed);
            throw failure;
        }
    }

    public interface OrganizationCleanup {

        void cleanup(OrganizationResource organization);

    }

    private record ResetOrganization(OrganizationRepresentation rep) implements OrganizationCleanup {

        @Override
        public void cleanup(OrganizationResource organization) {
            try (Response response = organization.update(rep)) {
                ApiUtil.expectStatus(response, "roll back organization '%s'".formatted(rep.getName()), Status.NO_CONTENT);
            }
        }

    }
}
