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
import Keycloak from './keycloak.js';

export interface KeycloakAuthorizationPromise {
	then(onGrant: (rpt: string) => void, onDeny: () => void, onError: () => void): void;
}

export interface AuthorizationRequest {
	/**
	 * An array of objects representing the resource and scopes.
	 */
	permissions?:ResourcePermission[],

	/**
	 * A permission ticket obtained from a resource server when using UMA authorization protocol.
	 */
	ticket?:string,

	/**
	 * A boolean value indicating whether the server should create permission requests to the resources
	 * and scopes referenced by a permission ticket. This parameter will only take effect when used together
	 * with the ticket parameter as part of a UMA authorization process.
	 */
	submitRequest?:boolean,

	/**
	 * Defines additional information about this authorization request in order to specify how it should be processed
	 * by the server.
	 */
	metadata?:AuthorizationRequestMetadata,

	/**
	 * Defines whether or not this authorization request should include the current RPT. If set to true, the RPT will
	 * be sent and permissions in the current RPT will be included in the new RPT. Otherwise, only the permissions referenced in this
	 * authorization request will be granted in the new RPT.
	 */
	incrementalAuthorization?:boolean
}

export interface AuthorizationRequestMetadata {
	/**
	 * A boolean value indicating to the server if resource names should be included in the RPT’s permissions.
	 * If false, only the resource identifier is included.
	 */
	responseIncludeResourceName?:any,

	/**
	 * An integer N that defines a limit for the amount of permissions an RPT can have. When used together with
	 * rpt parameter, only the last N requested permissions will be kept in the RPT.
	 */
	response_permissions_limit?:number
}

export interface ResourcePermission {
	/**
	 * The id or name of a resource.
	 */
	id:string,

	/**
	 * An array of strings where each value is the name of a scope associated with the resource.
	 */
	scopes?:string[]
}

/**
 * @deprecated Instead of importing 'KeycloakAuthorizationInstance' you can import 'KeycloakAuthorization' directly as a type.
 */
export type KeycloakAuthorizationInstance = KeycloakAuthorization;

/**
 * @deprecated Construct a KeycloakAuthorization instance using the `new` keyword instead.
 */
declare function KeycloakAuthorization(keycloak: Keycloak): KeycloakAuthorization;

declare class KeycloakAuthorization {
	/**
	 * Creates a new Keycloak client instance.
	 * @param config Path to a JSON config file or a plain config object.
	 */
	constructor(keycloak: Keycloak)

	rpt: any;
	config: { rpt_endpoint: string };

	/**
	 * Initializes the `KeycloakAuthorization` instance.
	 * @deprecated Initialization now happens automatically, calling this method is no longer required.
	 */
	init(): void;

	/**
	 * A promise that resolves when the `KeycloakAuthorization` instance is initialized.
	 * @deprecated Initialization now happens automatically, using this property is no longer required.
	 */
	ready: Promise<void>;

	/**
	 * This method enables client applications to better integrate with resource servers protected by a Keycloak
	 * policy enforcer using UMA protocol.
	 *
	 * The authorization request must be provided with a ticket.
	 *
	 * @param authorizationRequest An AuthorizationRequest instance with a valid permission ticket set.
	 * @returns A promise to set functions to be invoked on grant, deny or error.
	 */
	authorize(authorizationRequest: AuthorizationRequest): KeycloakAuthorizationPromise;

	/**
	 * Obtains all entitlements from a Keycloak server based on a given resourceServerId.
	 *
	 * @param resourceServerId The id (client id) of the resource server to obtain permissions from.
	 * @param authorizationRequest An AuthorizationRequest instance.
	 * @returns A promise to set functions to be invoked on grant, deny or error.
	 */
	entitlement(resourceServerId: string, authorizationRequest?: AuthorizationRequest): KeycloakAuthorizationPromise;
}

export default KeycloakAuthorization;

/**
 * @deprecated The 'KeycloakAuthorization' namespace is deprecated, use named imports instead.
 */
export as namespace KeycloakAuthorization;
