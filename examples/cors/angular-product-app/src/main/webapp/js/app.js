var module = angular.module('product', []);

var auth = {};
var logout = function(){
    console.log('*** LOGOUT');
    auth.loggedIn = false;
    auth.authz = null;
    window.location = auth.logoutUrl;
};


angular.element(document).ready(function ($http) {
    console.log("*** here");
    var keycloakAuth = new Keycloak('keycloak.json');
    auth.loggedIn = false;

    keycloakAuth.init({ onLoad: 'login-required' }).success(function () {
        console.log('here login');
        auth.loggedIn = true;
        auth.authz = keycloakAuth;
        auth.logoutUrl = keycloakAuth.authServerUrl + "/realms/" + keycloakAuth.realm + "/tokens/logout?redirect_uri=http://localhost:8080/angular-cors-product/index.html";
        module.factory('Auth', function() {
            return auth;
        });
        angular.bootstrap(document, ["product"]);
    }).error(function () {
            alert("failed to login");
        });

});

module.controller('GlobalCtrl', function($scope, $http) {
    $scope.products = [];
    $scope.roles = [];
    $scope.reloadData = function() {
        $http.get("http://localhost-db:8080/cors-database/products").success(function(data) {
            $scope.products = angular.fromJson(data);

        });

    };
    $scope.loadRoles = function() {
        $http.get("http://localhost-auth:8080/auth/admin/realms/" + auth.authz.realm + "/roles").success(function(data) {
            $scope.roles = angular.fromJson(data);

        });

    };
    $scope.addRole = function() {
        $http.post("http://localhost-auth:8080/auth/admin/realms/" + auth.authz.realm + "/roles", {name: 'stuff'}).success(function() {
            $scope.loadRoles();
        });

    };
    $scope.deleteRole = function() {
        $http.delete("http://localhost-auth:8080/auth/admin/realms/" + auth.authz.realm + "/roles/stuff").success(function() {
            $scope.loadRoles();
        });

    };

    $scope.loadServerInfo = function() {
        $http.get("http://localhost-auth:8080/auth/admin/serverinfo").success(function(data) {
            $scope.serverInfo = angular.fromJson(data);
        });

    };

    $scope.loadPublicRealmInfo = function() {
        $http.get("http://localhost-auth:8080/auth/realms/cors").success(function(data) {
            $scope.realm = angular.fromJson(data);
        });
    };

    $scope.logout = logout;
});


module.factory('authInterceptor', function($q, Auth) {
    return {
        request: function (config) {
            var deferred = $q.defer();
            if (Auth.authz.token) {
                Auth.authz.updateToken(5).success(function() {
                    config.headers = config.headers || {};
                    config.headers.Authorization = 'Bearer ' + Auth.authz.token;

                    deferred.resolve(config);
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
            if (response.status == 401) {
                console.log('session timeout?');
                logout();
            } else if (response.status == 403) {
                alert("Forbidden");
            } else if (response.status == 404) {
                alert("Not found");
            } else if (response.status) {
                if (response.data && response.data.errorMessage) {
                    alert(response.data.errorMessage);
                } else {
                    alert("An unexpected server error has occurred");
                }
            }
            return $q.reject(response);
        });
    };
});
