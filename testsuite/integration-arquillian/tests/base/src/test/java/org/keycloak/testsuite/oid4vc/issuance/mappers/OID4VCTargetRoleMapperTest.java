/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.oid4vc.issuance.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCTargetRoleMapper;
import org.keycloak.protocol.oid4vc.model.Role;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCTest;
import org.keycloak.testsuite.runonserver.RunOnServerException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.clientId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OID4VCTargetRoleMapperTest extends OID4VCTest {

	@Test
	public void testRoleMapping() throws Throwable {
		String token = getBearerToken(oauth);
		try {
			getTestingClient()
					.server(TEST_REALM_NAME)
					.run(session -> {
						Role expectedRole = new Role(Set.of("newRole"), "newClient");
						Map<String, Object> claimsMap = new HashMap<>();
						OID4VCTargetRoleMapper roleMapper = new OID4VCTargetRoleMapper(session);
						ProtocolMapperModel pmm = new ProtocolMapperModel();
						pmm.setConfig(Map.of(OID4VCTargetRoleMapper.CLIENT_CONFIG_KEY, "newClient"));
						roleMapper.setMapperModel(pmm, "jwt_vc");
						AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
						authenticator.setTokenString(token);
						UserSessionModel userSessionModel = authenticator.authenticate().session();
						roleMapper.setClaimsForSubject(claimsMap, userSessionModel);
						assertTrue("The roles should be included as a claim.", claimsMap.containsKey("roles"));
						if (claimsMap.get("roles") instanceof HashSet roles) {
							List<Role> rolesList = roles.stream().map(ro -> new ObjectMapper().convertValue(ro, Role.class)).toList();
							assertEquals("Only the requested client should be included in the roles set.", 1, rolesList.size());
							assertEquals(expectedRole, rolesList.get(0));
						} else {
							fail("Roles claim should be a role object.");
						}
					});
		} catch (RunOnServerException ros) {
			throw ros.getCause();
		}
	}


	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {

		ClientRepresentation newClient = new ClientRepresentation();
		newClient.setClientId("newClient");
		testRealm.getClients().add(newClient);

		Map<String, List<RoleRepresentation>> newClientRoles = testRealm.getRoles().getClient();
		newClientRoles.merge(
				"newClient",
				List.of(getRoleRepresentation("newRole", "newClient")),
				(existingRoles, newRoles) -> {
					List<RoleRepresentation> mergedRoles = new ArrayList<>(existingRoles);
					mergedRoles.addAll(newRoles);
					return mergedRoles;
				}
		);

		// Find existing client representation
		ClientRepresentation existingClient = testRealm.getClients().stream()
				.filter(client -> client.getClientId().equals(clientId))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Client with ID " + clientId + " not found in realm"));
		// Add role to existing client
		if (testRealm.getRoles() != null) {
			Map<String, List<RoleRepresentation>> clientRoles = testRealm.getRoles().getClient();
			clientRoles.merge(
					existingClient.getClientId(),
					List.of(getRoleRepresentation("testRole", existingClient.getClientId())),
					(existingRoles, newRoles) -> {
						List<RoleRepresentation> mergedRoles = new ArrayList<>(existingRoles);
						mergedRoles.addAll(newRoles);
						return mergedRoles;
					}
			);
		} else {
			testRealm.getRoles()
					.setClient(Map.of(existingClient.getClientId(),
							List.of(getRoleRepresentation("testRole", existingClient.getClientId()))));
		}

		List<UserRepresentation> realmUsers = Optional.ofNullable(testRealm.getUsers()).map(ArrayList::new)
				.orElse(new ArrayList<>());
		realmUsers.add(getUserRepresentation(Map.of(existingClient.getClientId(), List.of("testRole"), "newClient", List.of("newRole"))));
		testRealm.setUsers(realmUsers);
	}
}
