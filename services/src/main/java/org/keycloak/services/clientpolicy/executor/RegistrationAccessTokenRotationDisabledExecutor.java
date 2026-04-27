/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import org.keycloak.models.ClientRegistrationAccessTokenConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;

public class RegistrationAccessTokenRotationDisabledExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

	private final String providerId;
	private final KeycloakSession session;

	public RegistrationAccessTokenRotationDisabledExecutor(String providerId, KeycloakSession session) {
		this.providerId = providerId;
		this.session = session;
	}

	@Override
	public String getProviderId() {
		return providerId;
	}

	@Override
	public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
		if (session.getAttribute(ClientRegistrationAccessTokenConstants.ROTATION_ENABLED) == null){
			return;
		}
		session.setAttribute(ClientRegistrationAccessTokenConstants.ROTATION_ENABLED, false);
	}

}
