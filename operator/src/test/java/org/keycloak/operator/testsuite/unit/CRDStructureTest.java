package org.keycloak.operator.testsuite.unit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.operator.testsuite.integration.BaseOperatorTest;

import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class CRDStructureTest {

	@Test
	public void generatedOIDCCRDDoesNotContainUUIDs() throws IOException {
		assertNoUUIDInCRD("keycloakoidcclients.k8s.keycloak.org-v1.yml");
	}

	@Test
	public void generatedSAMLCRDDoesNotContainUUIDs() throws IOException {
		assertNoUUIDInCRD("keycloaksamlclients.k8s.keycloak.org-v1.yml");
	}

	@Test
	public void realmImportRawJsonFieldsRemainFreeForm() throws IOException {
		Path path = Path.of(BaseOperatorTest.TARGET_KUBERNETES_GENERATED_YML_FOLDER,
				"keycloakrealmimports.k8s.keycloak.org-v1.yml");
		JsonNode crd = Serialization.unmarshal(Files.readString(path), JsonNode.class);
		List<String> versionNames = new ArrayList<>();
		crd.path("spec").path("versions").forEach(version -> versionNames.add(version.path("name").asString()));
		assertThat(versionNames, containsInAnyOrder("v2alpha1", "v2beta1"));
		for (JsonNode version : crd.path("spec").path("versions")) {
			JsonNode realm = version.path("schema").path("openAPIV3Schema")
					.path("properties").path("spec").path("properties").path("realm").path("properties");
			for (String field : new String[] {"clientPolicies", "clientProfiles"}) {
				JsonNode schema = realm.path(field);
				assertTrue(schema.path("x-kubernetes-preserve-unknown-fields").asBoolean(), field);
				assertFalse(schema.has("properties"), field);
			}
		}
	}

	private static void assertNoUUIDInCRD(String crdFileName) throws IOException {
		Path crdPath = Path.of(BaseOperatorTest.TARGET_KUBERNETES_GENERATED_YML_FOLDER, crdFileName);
		JsonNode crdNode = Serialization.unmarshal(Files.readString(crdPath), JsonNode.class);

		JsonNode firstVersionSpecRoot = crdNode.path("spec").path("versions").get(0)
				.path("schema")
				.path("openAPIV3Schema")
				.path("properties")
				.path("spec");

		// Only validate "spec" schema content and intentionally ignore "status".
		String specSchemaContent = firstVersionSpecRoot.toString();
		assertThat(specSchemaContent, not(containsString("uuid")));
	}
}
