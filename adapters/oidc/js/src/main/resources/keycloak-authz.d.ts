/*
 * MIT License
 *
 * Copyright 2017 Brett Epps <https://github.com/eppsilon>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import * as Keycloak from 'keycloak';

export as namespace KeycloakAuthorization;

export = KeycloakAuthorization;

/**
 * Creates a new Keycloak client instance.
 * @param config Path to a JSON config file or a plain config object.
 */
declare function KeycloakAuthorization(keycloak: Keycloak.KeycloakInstance): KeycloakAuthorization.KeycloakAuthorizationInstance;

declare namespace KeycloakAuthorization {
	interface KeycloakAuthorizationPromise {
		then(onGrant: (rpt: string) => void, onDeny: () => void, onError: () => void): void;
	}

	interface KeycloakAuthorizationInstance {
		rpt: any;
		config: { rpt_endpoint: string };

		init(): void;

		/**
		 * This method enables client applications to better integrate with resource servers protected by a Keycloak
		 * policy enforcer.
		 *
		 * In this case, the resource server will respond with a 401 status code and a WWW-Authenticate header holding the
		 * necessary information to ask a Keycloak server for authorization data using both UMA and Entitlement protocol,
		 * depending on how the policy enforcer at the resource server was configured.
		 */
		authorize(wwwAuthenticateHeader: string): KeycloakAuthorizationPromise;

		/**
		 * Obtains all entitlements from a Keycloak server based on a given resourceServerId.
		 */
		entitlement(resourceServerId: string, entitlementRequest: {}): KeycloakAuthorizationPromise;
	}
}
