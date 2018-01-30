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

        this.init = function () {
            var request = new XMLHttpRequest();

            request.open('GET', keycloak.authServerUrl + '/realms/' + keycloak.realm + '/.well-known/uma2-configuration');
            request.onreadystatechange = function () {
                if (request.readyState == 4) {
                    if (request.status == 200) {
                        _instance.config = JSON.parse(request.responseText);
                    } else {
                        console.error('Could not obtain configuration from server.');
                    }
                }
            }

            request.send(null);
        };

        /**
         * This method enables client applications to better integrate with resource servers protected by a Keycloak
         * policy enforcer.
         *
         * In this case, the resource server will respond with a 401 status code and a WWW-Authenticate header holding the
         * necessary information to ask a Keycloak server for authorization data using both UMA and Entitlement protocol,
         * depending on how the policy enforcer at the resource server was configured.
         */
        this.authorize = function (wwwAuthenticateHeader) {
            this.then = function (onGrant, onDeny, onError) {
                if (wwwAuthenticateHeader.indexOf('UMA') != -1) {
                    var params = wwwAuthenticateHeader.split(',');

                    for (i = 0; i < params.length; i++) {
                        var param = params[i].split('=');

                        if (param[0] == 'ticket') {
                            var request = new XMLHttpRequest();
                            var ticket = param[1].substring(1, param[1].length - 1).trim();

                            request.open('POST', _instance.config.token_endpoint, true);
                            request.setRequestHeader("Content-type", "application/x-www-form-urlencoded");

                            request.onreadystatechange = function () {
                                if (request.readyState == 4) {
                                    var status = request.status;

                                    if (status >= 200 && status < 300) {
                                        var rpt = JSON.parse(request.responseText).rpt;
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

                            var params = "grant_type=urn:ietf:params:oauth:grant-type:uma-ticket&" +
                                        "client_id=" + keycloak.clientId + "&" +
                                        "claim_token=" + keycloak.token + "&" +
                                        "claim_token_format=urn:ietf:params:oauth:token-type:jwt&" +
                                        "ticket=" + ticket;

                            if (_instance.rpt) {
                                params += "&rpt=" + _instance.rpt;
                            }

                            request.send(params);
                        }
                    }
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

                request.onreadystatechange = function () {
                    if (request.readyState == 4) {
                        var status = request.status;

                        if (status >= 200 && status < 300) {
                            var rpt = JSON.parse(request.responseText).rpt;
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

                var params = "grant_type=urn:ietf:params:oauth:grant-type:uma-ticket&" +
                            "client_id=" + keycloak.clientId + "&" +
                            "claim_token=" + keycloak.token + "&" +
                            "claim_token_format=urn:ietf:params:oauth:token-type:jwt&" +
                            "audience=" + resourceServerId;

                if (!authorizationRequest) {
                    authorizationRequest = {
                        "resources": [],
                        "metadata": {}
                    };
                }

                if (authorizationRequest.resources) {
                    params += "&permissions=" + JSON.stringify({"resources": authorizationRequest.resources});
                }

                if (authorizationRequest.metadata) {
                    params += "&metadata=" + JSON.stringify(authorizationRequest.metadata);
                }

                if (_instance.rpt) {
                    params += "&rpt=" + _instance.rpt;
                }

                request.send(params);
            };

            return this;
        };

        this.init(this);
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