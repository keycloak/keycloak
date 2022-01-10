/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

var module = angular.module('app', []);

angular.element(document).ready(function ($http) {
    var keycloakAuth = new Keycloak('keycloak.json');

    keycloakAuth.init({ onLoad: 'login-required' }).then(function () {
        module.factory('Auth', function() {
            var Auth = {};

            Auth.logout = function() {
                keycloakAuth.logout();
            }

            Auth.getIdentity = function() {
                return keycloakAuth.idTokenParsed;
            }

            Auth.getToken = function() {
                return keycloakAuth.token;
            }

            Auth.refreshToken = function() {
                return window.location = keycloakAuth.createLoginUrl({
                    idpHint: 'google'
                });
            }

            return Auth;
        });

        module.factory('authInterceptor', function($q) {
            return {
                request: function (config) {
                    var deferred = $q.defer();

                    config.headers = config.headers || {};

                    if (!config.headers.Authorization) {
                        config.headers.Authorization = 'Bearer ' + keycloakAuth.token;
                    }

                    deferred.resolve(config);

                    if (keycloakAuth.token) {
                        keycloakAuth.updateToken(5).then(function() {
                        }).catch(function() {
                            deferred.reject('Failed to refresh token');
                        });
                    }

                    return deferred.promise;
                }
            };
        });

        module.config(function($httpProvider) {
            $httpProvider.responseInterceptors.push('errorInterceptor');
            $httpProvider.interceptors.push('authInterceptor');

        });

        module.factory('errorInterceptor', function($q) {
            return function(promise) {
                return promise.then(function(response) {
                    return response;
                }, function(response) {
                    return $q.reject(response);
                });
            };
        });

        angular.bootstrap(document, ["app"]);
    }).catch(function () {
        window.location.reload();
    });
});

module.controller('GlobalCtrl', function($scope, $http, $location, Auth) {
    $scope.logout = function() {
        Auth.logout();
    }

    $scope.identity = Auth.getIdentity();

    $scope.loadSocialProfile = function() {
        $http.get('/auth/realms/google-identity-provider-realm/broker/google/token').success(function(data) {
            var accessToken = data.access_token;

            var req = {
                method: 'GET',
                url: 'https://www.googleapis.com/plus/v1/people/me',
                headers: {
                    'Authorization': 'Bearer ' + accessToken
                }
            };

            $http(req)
                .success(function(profile) {
                    $scope.socialProfile = profile;
                })
                .error(function(data, status, headers, config) {
                    $scope.socialProfile = 'Could not obtain social profile. Trying to refresh your token.';
                    Auth.refreshToken();
                });
        });
    }
});