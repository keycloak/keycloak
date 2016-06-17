var module = angular.module('photoz-uma', ['ngRoute', 'ngResource']);

var Identity = {};

angular.element(document).ready(function ($http) {
    var keycloakAuth = new Keycloak('keycloak.json');
    Identity.loggedIn = false;
    keycloakAuth.init({onLoad: 'login-required'}).success(function () {
        Identity.loggedIn = true;
        Identity.authz = keycloakAuth;
        Identity.logout = function () {
            Identity.loggedIn = false;
            Identity.claim = {};
            Identity.authc = null;
            window.location = this.authz.authServerUrl + "/realms/photoz-uma/protocol/openid-connect/logout?redirect_uri=http://localhost:8080/photoz-uma-html5-client/index.html";
            Identity.authz = null;
        };
        Identity.claim = {};
        Identity.claim.name = Identity.authz.idTokenParsed.name;
        Identity.hasRole = function (name) {
            if (Identity.authz && Identity.authz.hasRealmRole(name)) {
                return true;
            }
            return false;
        };
        Identity.isAdmin = function () {
            return this.hasRole("admin");
        };
        Identity.authc = {};
        Identity.authc.token = Identity.authz.token;
        module.factory('Identity', function () {
            return Identity;
        });
        angular.bootstrap(document, ["photoz-uma"]);
    }).error(function () {
        window.location.reload();
    });
});
module.controller('GlobalCtrl', function ($scope, $http, $route, $location, Album, Identity) {
    Album.query(function (albums) {
        $scope.albums = albums;
    });

    $scope.Identity = Identity;

    $scope.deleteAlbum = function (album) {
        new Album(album).$delete({id: album.id}, function () {
            $route.reload();
        });
    }
});
module.controller('TokenCtrl', function ($scope, Identity) {
    $scope.showRpt = function () {
        document.getElementById("output").innerHTML = JSON.stringify(jwt_decode(Identity.uma.rpt.rpt), null, '  ');
    }

    $scope.showAccessToken = function () {
        document.getElementById("output").innerHTML = JSON.stringify(jwt_decode(Identity.authc.token), null, '  ');
    }

    $scope.requestEntitlements = function () {
        var request = new XMLHttpRequest();

        request.open("GET", "http://localhost:8080/auth/realms/photoz-uma/authz/entitlement/photoz-uma-restful-api", true);
        request.setRequestHeader("Authorization", "Bearer " + Identity.authc.token);
        request.onreadystatechange = function () {
            if (request.readyState == 4 && request.status == 200) {
                Identity.uma.rpt = JSON.parse(request.responseText);
            }
        }

        request.send(null);
    }
});
module.controller('AlbumCtrl', function ($scope, $http, $routeParams, $location, Album) {
    $scope.album = {};
    if ($routeParams.id) {
        $scope.album = Album.get({id: $routeParams.id});
    }
    $scope.create = function () {
        var newAlbum = new Album($scope.album);
        newAlbum.$save({}, function (data) {
            $location.path('/');
        });
    };
});
module.controller('ProfileCtrl', function ($scope, $http, $routeParams, $location, Profile) {
    $scope.profile = Profile.get();
});
module.controller('AdminAlbumCtrl', function ($scope, $http, $route, AdminAlbum, Album) {
    $scope.albums = {};
    $http.get('/photoz-uma-restful-api/admin/album').success(function (data) {
        $scope.albums = data;
    });
    $scope.deleteAlbum = function (album) {
        var newAlbum = new Album(album);
        newAlbum.$delete({id: album.id}, function () {
            $route.reload();
        });
    }
});
module.factory('Album', ['$resource', function ($resource) {
    return $resource('http://localhost:8080/photoz-uma-restful-api/album/:id');
}]);
module.factory('Profile', ['$resource', function ($resource) {
    return $resource('http://localhost:8080/photoz-uma-restful-api/profile');
}]);
module.factory('AdminAlbum', ['$resource', function ($resource) {
    return $resource('http://localhost:8080/photoz-uma-restful-api/admin/album/:id');
}]);
module.factory('authInterceptor', function ($q, $injector, $timeout, Identity) {
    return {
        request: function (request) {
            document.getElementById("output").innerHTML = '';
            if (Identity.uma && Identity.uma.rpt && request.url.indexOf('/authorize') == -1) {
                retries = 0;
                request.headers.Authorization = 'Bearer ' + Identity.uma.rpt.rpt;
            } else {
                request.headers.Authorization = 'Bearer ' + Identity.authc.token;
            }
            return request;
        },
        responseError: function (rejection) {
            if (rejection.status == 403 || rejection.status == 401) {
                var retry = (!rejection.config.retry ||  rejection.config.retry < 1);

                if (!retry) {
                    document.getElementById("output").innerHTML = 'You can not access or perform the requested operation on this resource.';
                    return $q.reject(rejection);
                }

                if (rejection.config.url.indexOf('/authorize') == -1 && retry) {
                    if (rejection.status == 401) {
                        console.log("Here");
                        var authenticateHeader = rejection.headers('WWW-Authenticate');

                        if (authenticateHeader.startsWith('UMA')) {
                            var params = authenticateHeader.split(',');

                            for (i = 0; i < params.length; i++) {
                                var param = params[i].split('=');

                                if (param[0] == 'ticket') {
                                    var ticket = param[1].substring(1, param[1].length - 1).trim();

                                    var data = JSON.stringify({
                                        ticket: ticket,
                                        rpt: Identity.uma ? Identity.uma.rpt.rpt : ""
                                    });

                                    var $http = $injector.get("$http");

                                    var deferred = $q.defer();

                                    $http.post('http://localhost:8080/auth/realms/photoz-uma/authz/authorize', data, {headers: {"Authorization": "Bearer " + Identity.authc.token}})
                                        .then(function (authzResponse) {
                                            if (authzResponse.data) {
                                                Identity.uma = {};
                                                Identity.uma.rpt = authzResponse.data;
                                            }
                                            deferred.resolve(rejection);
                                        }, function (authzResponse) {
                                            document.getElementById("output").innerHTML = 'You can not access or perform the requested operation on this resource.';
                                        });

                                    var promise = deferred.promise;

                                    return promise.then(function (res) {
                                        if (!res.config.retry) {
                                            res.config.retry = 1;
                                        } else {
                                            res.config.retry++;
                                        }
                                        return $http(res.config).then(function (response) {
                                            return response;
                                        });
                                    });
                                }
                            }
                        }
                    }
                }
            }

            return $q.reject(rejection);
        }
    };
});
module.config(function ($httpProvider, $routeProvider) {
    $httpProvider.interceptors.push('authInterceptor');
    $routeProvider.when('/', {
        templateUrl: 'partials/home.html',
        controller: 'GlobalCtrl'
    }).when('/album/create', {
        templateUrl: 'partials/album/create.html',
        controller: 'AlbumCtrl',
    }).when('/album/:id', {
        templateUrl: 'partials/album/detail.html',
        controller: 'AlbumCtrl',
    }).when('/admin/album', {
        templateUrl: 'partials/admin/albums.html',
        controller: 'AdminAlbumCtrl',
    }).when('/profile', {
        templateUrl: 'partials/profile.html',
        controller: 'ProfileCtrl',
    });
});