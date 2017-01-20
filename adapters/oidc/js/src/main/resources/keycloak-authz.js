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

    var KeycloakAuthorization = function (keycloak) {
        var _instance = this;
        this.rpt = null;

        this.init = function () {
            var request = new XMLHttpRequest();

            request.open('GET', keycloak.authServerUrl + '/realms/' + keycloak.realm + '/.well-known/uma-configuration');
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

                            request.open('POST', _instance.config.rpt_endpoint, true);
                            request.setRequestHeader('Content-Type', 'application/json')
                            request.setRequestHeader('Authorization', 'Bearer ' + keycloak.token)

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

                            var ticket = param[1].substring(1, param[1].length - 1).trim();

                            request.send(JSON.stringify(
                                {
                                    ticket: ticket,
                                    rpt: _instance.rpt
                                }
                            ));
                        }
                    }
                } else if (wwwAuthenticateHeader.indexOf('KC_ETT') != -1) {
                    var params = wwwAuthenticateHeader.substring('KC_ETT'.length).trim().split(',');
                    var clientId = null;

                    for (i = 0; i < params.length; i++) {
                        var param = params[i].split('=');

                        if (param[0] == 'realm') {
                            clientId = param[1].substring(1, param[1].length - 1).trim();
                        }
                    }

                    _instance.entitlement(clientId).then(onGrant, onDeny, onError);
                }
            };

            return this;
        };

        /**
         * Obtains all entitlements from a Keycloak Server based on a give resourceServerId.
         */
        this.entitlement = function (resourceSeververId, entitlementRequest     ) {
            this.then = function (onGrant, onDeny, onError) {
                var request = new XMLHttpRequest();



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

                var erJson = null

                if(entitlementRequest) {
                    request.open('POST', keycloak.authServerUrl + '/realms/' + keycloak.realm + '/authz/entitlement/' + resourceSeververId, true);
                    request.setRequestHeader("Content-type", "application/json");
                    erJson = JSON.stringify(entitlementRequest)
                } else {
                    request.open('GET', keycloak.authServerUrl + '/realms/' + keycloak.realm + '/authz/entitlement/' + resourceSeververId, true);
                }

                request.setRequestHeader('Authorization', 'Bearer ' + keycloak.token)
                request.send(erJson);

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