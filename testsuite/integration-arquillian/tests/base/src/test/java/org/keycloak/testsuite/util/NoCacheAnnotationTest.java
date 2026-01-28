/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Réda Housni Alaoui
 */
@AuthServerContainerExclude(value = AuthServerContainerExclude.AuthServer.UNDERTOW, details = "org.jboss.resteasy.reactive.NoCache is only supported by Quarkus")
public class NoCacheAnnotationTest extends AbstractKeycloakTest {
	@Override
	public void addTestRealms(List<RealmRepresentation> testRealms) {
		// Do nothing
	}

	@Test
	public void noCacheAnnotationOverridesProgrammaticCacheControlBehaviour() {
		Response response = getTestingClient().testing().getNoCacheAnnotatedEndpointResponse(2000L);
		Assert.assertEquals(204, response.getStatus());
		Assert.assertEquals("no-cache", response.getHeaderString("cache-control"));
	}
}
