var module = angular.module('app', []);

angular.element(document).ready(function ($http) {
    var keycloakAuth = new Keycloak('keycloak.json');

    keycloakAuth.init({ onLoad: 'login-required' }).success(function () {
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
                var url = keycloakAuth.createLoginUrl({
                    idpHint: 'twitter'
                });

                console.log(url);

                window.location = url;
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
                        keycloakAuth.updateToken(5).success(function() {
                        }).error(function() {
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
    }).error(function () {
        window.location.reload();
    });
});

module.controller('GlobalCtrl', function($scope, $http, $location, Auth) {
    $scope.logout = function() {
        Auth.logout();
    }

    $scope.identity = Auth.getIdentity();

    $scope.loadSocialProfile = function() {
        $http.get('/twitter-authentication/twitter/showUser')
            .success(function(profile) {
                $scope.socialProfile = profile;
            })
            .error(function(data, status, headers, config) {
                $scope.socialProfile = 'Could not obtain social profile. Trying to refresh your token.';
                Auth.refreshToken();
            });
    }
});