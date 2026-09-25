package org.keycloak.operator.controllers;

import java.util.Optional;
import java.util.Set;

import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.crds.v2beta1.realmimport.KeycloakRealmImport;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnerReferenceMappingTest {

    private static final String LEGACY_API_VERSION = "k8s.keycloak.org/v2alpha1";

    @Test
    void shouldMapLegacyOwnerReferenceForAdminSecret() {
        var mapper = new TestKeycloakAdminSecretDependentResource()
                .secondaryToPrimaryMapper(contextFor(Keycloak.class));

        assertPrimaryResourceId(mapper, secretOwnedBy(Keycloak.class, "example"));
    }

    @Test
    void shouldMapLegacyOwnerReferenceForRealmImportJob() {
        var mapper = new TestKeycloakRealmImportJobDependentResource()
                .secondaryToPrimaryMapper(contextFor(KeycloakRealmImport.class));

        assertPrimaryResourceId(mapper, jobOwnedBy(KeycloakRealmImport.class, "example-realm-import"));
    }

    private static <R extends HasMetadata> void assertPrimaryResourceId(Optional<SecondaryToPrimaryMapper<R>> mapper, R secondary) {
        assertTrue(mapper.isPresent());

        Set<ResourceID> resourceIds = mapper.orElseThrow().toPrimaryResourceIDs(secondary);
        assertEquals(1, resourceIds.size());

        ResourceID resourceId = resourceIds.iterator().next();
        assertEquals(secondary.getMetadata().getOwnerReferences().get(0).getName(), resourceId.getName());
        assertEquals(Optional.of(secondary.getMetadata().getNamespace()), resourceId.getNamespace());
    }

    @SuppressWarnings("unchecked")
    private static <P extends HasMetadata> EventSourceContext<P> contextFor(Class<P> primaryClass) {
        EventSourceContext<P> context = Mockito.mock(EventSourceContext.class);
        Mockito.when(context.getPrimaryResourceClass()).thenReturn(primaryClass);
        return context;
    }

    private static Secret secretOwnedBy(Class<? extends HasMetadata> primaryClass, String primaryName) {
        return new SecretBuilder()
                .withNewMetadata()
                .withName("secret")
                .withNamespace("test")
                .addToOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(LEGACY_API_VERSION)
                        .withKind(HasMetadata.getKind(primaryClass))
                        .withName(primaryName)
                        .build())
                .endMetadata()
                .build();
    }

    private static Job jobOwnedBy(Class<? extends HasMetadata> primaryClass, String primaryName) {
        return new JobBuilder()
                .withNewMetadata()
                .withName("job")
                .withNamespace("test")
                .addToOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(LEGACY_API_VERSION)
                        .withKind(HasMetadata.getKind(primaryClass))
                        .withName(primaryName)
                        .build())
                .endMetadata()
                .build();
    }

    private static final class TestKeycloakAdminSecretDependentResource extends KeycloakAdminSecretDependentResource {

        private Optional<SecondaryToPrimaryMapper<Secret>> secondaryToPrimaryMapper(EventSourceContext<Keycloak> context) {
            return getSecondaryToPrimaryMapper(context);
        }
    }

    private static final class TestKeycloakRealmImportJobDependentResource extends KeycloakRealmImportJobDependentResource {

        private Optional<SecondaryToPrimaryMapper<Job>> secondaryToPrimaryMapper(EventSourceContext<KeycloakRealmImport> context) {
            return getSecondaryToPrimaryMapper(context);
        }
    }
}
