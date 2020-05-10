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

(function( window, undefined ) {

    var KeycloakAuthorization = function (keycloak, options) {
        var _instance = this;
        this.rpt = null;

        var resolve = function () {};
        var reject = function () {};

        // detects if browser supports promises
        if (typeof Promise !== "undefined" && Promise.toString().indexOf("[native code]") !== -1) {
            this.ready = new Promise(function (res, rej) {
              resolve = res;
              reject = rej;
            });
        }

        this.init = function () {
            var request = new XMLHttpRequest();

            request.open('GET', keycloak.authServerUrl + '/realms/' + keycloak.realm + '/.well-known/uma2-configuration');
            request.onreadystatechange = function () {
                if (request.readyState == 4) {
                    if (request.status == 200) {
                        _instance.config = JSON.parse(request.responseText);
                        resolve();
                    } else {
                        console.error('Could not obtain configuration from server.');
                        reject();
                    }
                }
            }

            request.send(null);
        };

        /**
         * This method enables client applications to better integrate with resource servers protected by a Keycloak
         * policy enforcer using UMA protocol.
         *
         * The authorization request must be provided with a ticket.
         */
        this.authorize = function (authorizationRequest) {
            this.then = function (onGrant, onDeny, onError) {
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
            this.then = function (onGrant, onDeny, onError) {
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

                for (i = 0; i < permissions.length; i++) {
                    var resource = permissions[i];
                    var permission = resource.id;

                    if (resource.scopes && resource.scopes.length > 0) {
                        permission += "#";
                        for (j = 0; j < resource.scopes.length; j++) {
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

        this.init(this);

        return this;
    };

    if ( typeof module === "object" && module && typeof module.exports === "object" ) {
        module.exports = KeycloakAuthorization;
    } else {
        window.KeycloakAuthorization = KeycloakAuthorization;

        if ( typeof define === "function" && define.amd ) {
            define( "keycloak-authorization", [], function () { return KeycloakAuthorization; } );
        }
    }
})( window );