/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

var KeycloakAuthorization = function (keycloak, options) {
    var _instance = this;
    this.rpt = null;

    // Only here for backwards compatibility, as the configuration is now loaded on demand.
    // See:
    // - https://github.com/keycloak/keycloak/pull/6619
    // - https://issues.redhat.com/browse/KEYCLOAK-10894
    // TODO: Remove both `ready` property and `init` method in a future version
    this.ready = Promise.resolve();
    this.init = () => {};

    /** @type {Promise<unknown> | undefined} */
    let configPromise;

    /**
     * Initializes the configuration or re-uses the existing one if present.
     * @returns {Promise<void>} A promise that resolves when the configuration is loaded.
     */
    async function initializeConfigIfNeeded() {
        if (_instance.config) {
            return _instance.config;
        }

        if (configPromise) {
            return await configPromise;
        }

        if (!keycloak.didInitialize) {
            throw new Error('The Keycloak instance has not been initialized yet.');
        }
        
        configPromise = loadConfig(keycloak.authServerUrl, keycloak.realm);
        _instance.config = await configPromise;
    }

    /**
     * This method enables client applications to better integrate with resource servers protected by a Keycloak
     * policy enforcer using UMA protocol.
     *
     * The authorization request must be provided with a ticket.
     */
    this.authorize = function (authorizationRequest) {
        this.then = async function (onGrant, onDeny, onError) {
            try {
                await initializeConfigIfNeeded();
            } catch (error) {
                handleError(error, onError);
                return;
            }

            if (authorizationRequest && authorizationRequest.ticket) {
                var request = new XMLHttpRequest();

                request.open('POST', _instance.config.token_endpoint, true);
                request.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                request.setRequestHeader('Authorization', 'Bearer ' + keycloak.token);

                request.onreadystatechange = function () {
                    if (request.readyState == 4) {
                        var status = request.status;

                        if (status >= 200 && status < 300) {
                            var rpt = JSON.parse(request.responseText).access_token;
                            _instance.rpt = rpt;
                            onGrant(rpt);
                        } else if (status == 403) {
                            if (onDeny) {
                                onDeny();
                            } else {
                                console.error('Authorization request was denied by the server.');
                            }
                        } else {
                            if (onError) {
                                onError();
                            } else {
                                console.error('Could not obtain authorization data from server.');
                            }
                        }
                    }
                };

                var params = "grant_type=urn:ietf:params:oauth:grant-type:uma-ticket&client_id=" + keycloak.clientId + "&ticket=" + authorizationRequest.ticket;

                if (authorizationRequest.submitRequest != undefined) {
                    params += "&submit_request=" + authorizationRequest.submitRequest;
                }

                var metadata = authorizationRequest.metadata;

                if (metadata) {
                    if (metadata.responseIncludeResourceName) {
                        params += "&response_include_resource_name=" + metadata.responseIncludeResourceName;
                    }
                    if (metadata.responsePermissionsLimit) {
                        params += "&response_permissions_limit=" + metadata.responsePermissionsLimit;
                    }
                }

                if (_instance.rpt && (authorizationRequest.incrementalAuthorization == undefined || authorizationRequest.incrementalAuthorization)) {
                    params += "&rpt=" + _instance.rpt;
                }

                request.send(params);
            }
        };

        return this;
    };

    /**
     * Obtains all entitlements from a Keycloak Server based on a given resourceServerId.
     */
    this.entitlement = function (resourceServerId, authorizationRequest) {
        this.then = async function (onGrant, onDeny, onError) {
            try {
                await initializeConfigIfNeeded();
            } catch (error) {
                handleError(error, onError);
                return;
            }

            var request = new XMLHttpRequest();

            request.open('POST', _instance.config.token_endpoint, true);
            request.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            request.setRequestHeader('Authorization', 'Bearer ' + keycloak.token);

            request.onreadystatechange = function () {
                if (request.readyState == 4) {
                    var status = request.status;

                    if (status >= 200 && status < 300) {
                        var rpt = JSON.parse(request.responseText).access_token;
                        _instance.rpt = rpt;
                        onGrant(rpt);
                    } else if (status == 403) {
                        if (onDeny) {
                            onDeny();
                        } else {
                            console.error('Authorization request was denied by the server.');
                        }
                    } else {
                        if (onError) {
                            onError();
                        } else {
                            console.error('Could not obtain authorization data from server.');
                        }
                    }
                }
            };

            if (!authorizationRequest) {
                authorizationRequest = {};
            }

            var params = "grant_type=urn:ietf:params:oauth:grant-type:uma-ticket&client_id=" + keycloak.clientId;

            if (authorizationRequest.claimToken) {
                params += "&claim_token=" + authorizationRequest.claimToken;

                if (authorizationRequest.claimTokenFormat) {
                    params += "&claim_token_format=" + authorizationRequest.claimTokenFormat;
                }
            }

            params += "&audience=" + resourceServerId;

            var permissions = authorizationRequest.permissions;

            if (!permissions) {
                permissions = [];
            }

            for (var i = 0; i < permissions.length; i++) {
                var resource = permissions[i];
                var permission = resource.id;

                if (resource.scopes && resource.scopes.length > 0) {
                    permission += "#";
                    for (var j = 0; j < resource.scopes.length; j++) {
                        var scope = resource.scopes[j];
                        if (permission.indexOf('#') != permission.length - 1) {
                            permission += ",";
                        }
                        permission += scope;
                    }
                }

                params += "&permission=" + permission;
            }

            var metadata = authorizationRequest.metadata;

            if (metadata) {
                if (metadata.responseIncludeResourceName) {
                    params += "&response_include_resource_name=" + metadata.responseIncludeResourceName;
                }
                if (metadata.responsePermissionsLimit) {
                    params += "&response_permissions_limit=" + metadata.responsePermissionsLimit;
                }
            }

            if (_instance.rpt) {
                params += "&rpt=" + _instance.rpt;
            }

            request.send(params);
        };

        return this;
    };

    return this;
};

/**
 * Obtains the configuration from the server.
 * @param {string} serverUrl The URL of the Keycloak server.
 * @param {string} realm The realm name.
 * @returns {Promise<unknown>} A promise that resolves when the configuration is loaded.
 */
async function loadConfig(serverUrl, realm) {
    const url = `${serverUrl}/realms/${encodeURIComponent(realm)}/.well-known/uma2-configuration`;

    try {
        return await fetchJSON(url);
    } catch (error) {
        throw new Error('Could not obtain configuration from server.', { cause: error });
    }
}

/**
 * Fetches the JSON data from the given URL.
 * @param {string} url The URL to fetch the data from.
 * @returns {Promise<unknown>} A promise that resolves when the data is loaded.
 */
async function fetchJSON(url) {
    let response;

    try {
        response = await fetch(url);
    } catch (error) {
        throw new Error('Server did not respond.', { cause: error });
    }

    if (!response.ok) {
        throw new Error('Server responded with an invalid status.');
    }

    try {
        return await response.json();
    } catch (error) {
        throw new Error('Server responded with invalid JSON.', { cause: error });
    }
}

/**
 * @param {unknown} error 
 * @param {((error: unknown) => void) | undefined} handler 
 */
function handleError(error, handler) {
    if (handler) {
        handler(error);
    } else {
        console.error(message, error);
    }
}

export default KeycloakAuthorization;
