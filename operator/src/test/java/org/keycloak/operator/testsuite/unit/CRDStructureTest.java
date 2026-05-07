package org.keycloak.operator.testsuite.unit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.keycloak.operator.testsuite.integration.BaseOperatorTest;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
